package pe.upeu.agrosierra.servicio

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.*
import pe.upeu.agrosierra.web.dto.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class ErrorNegocio(val estado: HttpStatus, override val message: String,
    val campos: Map<String, String> = emptyMap()) : RuntimeException(message)

@Service
class AgroServicio(private val lotes: LoteRepository, private val labores: LaborRepository,
    private val usuarios: UsuarioRepository) {

    @Transactional(readOnly = true)
    fun usuarioActual(auth: Authentication): Usuario = usuarios.findById(auth.name.toLong())
        .orElseThrow { ErrorNegocio(HttpStatus.UNAUTHORIZED, "Usuario no disponible") }

    private fun paginar(pagina: Int, tamanio: Int, sort: Sort = Sort.unsorted()): PageRequest {
        if (pagina < 0 || tamanio !in 1..100) throw ErrorNegocio(HttpStatus.BAD_REQUEST,
            "Paginación inválida", mapOf("pagina" to "Debe ser 0 o mayor", "tamanio" to "Debe ser entre 1 y 100"))
        return PageRequest.of(pagina, tamanio, sort)
    }

    @Transactional(readOnly = true)
    fun listarLotes(pagina: Int, tamanio: Int, valle: String?, estado: String?): PaginaRespuesta<LoteRespuesta> {
        val cerrada = when (estado?.uppercase()) {
            null -> null
            "CERRADA" -> true
            "ABIERTA" -> false
            else -> throw ErrorNegocio(HttpStatus.BAD_REQUEST, "Estado inválido",
                mapOf("estado" to "Use ABIERTA o CERRADA"))
        }
        return lotes.filtrar(valle?.takeIf { it.isNotBlank() }, cerrada,
            paginar(pagina, tamanio, Sort.by("id").ascending())).envolver { it.aRespuesta() }
    }

    @Transactional(readOnly = true)
    fun buscarLote(id: Long): Lote = lotes.findById(id)
        .orElseThrow { ErrorNegocio(HttpStatus.NOT_FOUND, "Lote no encontrado") }

    @Transactional(readOnly = true)
    fun listarLabores(loteId: Long, pagina: Int, tamanio: Int): PaginaRespuesta<LaborRespuesta> {
        buscarLote(loteId)
        return labores.findByLoteId(loteId,
            paginar(pagina, tamanio, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))))
            .envolver { it.aRespuesta() }
    }

    private fun validarDosis(lote: Lote, tipo: TipoLabor, cantidad: Double) {
        if (tipo == TipoLabor.FUMIGACION && cantidad > lote.hectareas * lote.dosisMaximaPorHectarea) {
            val maximo = lote.hectareas * lote.dosisMaximaPorHectarea
            throw ErrorNegocio(HttpStatus.BAD_REQUEST, "Dosis de fumigación excedida",
                mapOf("cantidad" to "Máximo permitido para el lote: $maximo"))
        }
    }

    private fun exigirAbierto(lote: Lote) {
        if (lote.campaniaCerrada) throw ErrorNegocio(HttpStatus.CONFLICT,
            "La campaña del lote está cerrada; no se pueden registrar ni corregir labores")
    }

    @Transactional
    fun crear(datos: CrearLaborPeticion, auth: Authentication): LaborRespuesta {
        val lote = buscarLote(datos.loteId!!)
        exigirAbierto(lote) // RN-01
        validarDosis(lote, datos.tipo!!, datos.cantidad!!) // RN-04
        val labor = Labor(lote = lote, tipo = datos.tipo, fecha = datos.fecha!!,
            autor = usuarioActual(auth), cantidad = datos.cantidad,
            cantidadJornales = datos.cantidadJornales,
            observacion = datos.observacion.trim())
        return labores.save(labor).aRespuesta()
    }

    @Transactional
    fun editar(id: Long, datos: EditarLaborPeticion, auth: Authentication): LaborRespuesta {
        val labor = labores.findById(id)
            .orElseThrow { ErrorNegocio(HttpStatus.NOT_FOUND, "Labor no encontrada") }
        exigirAbierto(labor.lote) // RN-01
        val usuario = usuarioActual(auth)
        if (!usuario.rol.editaTodasLabores &&
            (labor.autor.id != usuario.id || Instant.now().isAfter(labor.registradoEn.plus(24, ChronoUnit.HOURS)))) {
            throw ErrorNegocio(HttpStatus.FORBIDDEN,
                "Solo puedes corregir tus propias labores durante las primeras 24 horas") // RN-02
        }
        validarDosis(labor.lote, datos.tipo!!, datos.cantidad!!) // RN-04 también al corregir
        labor.tipo = datos.tipo
        labor.fecha = datos.fecha!!
        labor.cantidad = datos.cantidad
        labor.cantidadJornales = datos.cantidadJornales
        labor.observacion = datos.observacion.trim()
        return labores.save(labor).aRespuesta()
    }

    @Transactional
    fun eliminar(id: Long) {
        val labor = labores.findById(id)
            .orElseThrow { ErrorNegocio(HttpStatus.NOT_FOUND, "Labor no encontrada") }
        labores.delete(labor)
    }

    @Transactional
    fun cerrarCampania(id: Long): LoteRespuesta {
        val lote = buscarLote(id)
        if (lote.campaniaCerrada) throw ErrorNegocio(HttpStatus.CONFLICT, "La campaña ya está cerrada")
        if (!labores.existsByLoteIdAndTipo(id, TipoLabor.COSECHA)) {
            throw ErrorNegocio(HttpStatus.CONFLICT, "Se requiere una labor de Cosecha para cerrar la campaña") // RN-03
        }
        lote.campaniaCerrada = true
        return lotes.save(lote).aRespuesta()
    }
}
