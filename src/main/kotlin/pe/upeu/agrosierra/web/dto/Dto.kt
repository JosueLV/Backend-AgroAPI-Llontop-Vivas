package pe.upeu.agrosierra.web.dto

import jakarta.validation.constraints.*
import pe.upeu.agrosierra.dominio.*
import java.time.LocalDate

data class LoginPeticion(@field:NotBlank val usuario: String, @field:NotBlank val contrasena: String)
data class LoginRespuesta(val token: String, val tipo: String = "Bearer", val expiraEnSegundos: Long = 3600,
    val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>)
data class UsuarioRespuesta(val id: Long, val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>)
fun Usuario.aRespuesta() = UsuarioRespuesta(id!!, usuario, nombre, rol.nombre, rol.privilegios.toSortedSet())

data class LoteRespuesta(val id: Long, val codigo: String, val nombre: String, val hectareas: Double,
    val cultivo: String, val valle: String, val campaniaCerrada: Boolean, val dosisMaximaPorHectarea: Double)
fun Lote.aRespuesta() = LoteRespuesta(id!!, codigo, nombre, hectareas, cultivo, valle, campaniaCerrada, dosisMaximaPorHectarea)

data class LaborRespuesta(val id: Long, val loteId: Long, val tipo: TipoLabor, val fecha: LocalDate,
    val responsable: String, val cantidad: Double, val cantidadJornales: Int,
    val observacion: String, val pendiente: Boolean = false)
fun Labor.aRespuesta() = LaborRespuesta(id!!, lote.id!!, tipo, fecha, autor.nombre, cantidad, cantidadJornales, observacion)

data class CrearLaborPeticion(@field:NotNull val loteId: Long?, @field:NotNull val tipo: TipoLabor?,
    @field:NotNull val fecha: LocalDate?, @field:NotNull @field:Positive val cantidad: Double?,
    @field:Min(1) val cantidadJornales: Int = 1,
    @field:Size(max = 500) val observacion: String = "")
data class EditarLaborPeticion(@field:NotNull val tipo: TipoLabor?, @field:NotNull val fecha: LocalDate?,
    @field:NotNull @field:Positive val cantidad: Double?,
    @field:Min(1) val cantidadJornales: Int = 1,
    @field:Size(max = 500) val observacion: String = "")

data class PaginaRespuesta<T>(val contenido: List<T>, val pagina: Int, val tamanio: Int,
    val totalElementos: Long, val totalPaginas: Int, val ultima: Boolean)
fun <T, R> org.springframework.data.domain.Page<T>.envolver(mapper: (T) -> R): PaginaRespuesta<R> =
    PaginaRespuesta(content.map(mapper), number, size, totalElements, totalPages, isLast)
