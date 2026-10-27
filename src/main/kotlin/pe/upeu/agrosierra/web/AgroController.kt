package pe.upeu.agrosierra.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.servicio.AgroServicio
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1")
class AgroController(private val agro: AgroServicio) {
    @GetMapping("/lotes")
    fun lotes(@RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int,
        @RequestParam(required = false) valle: String?,
        @RequestParam(required = false) estado: String?) = agro.listarLotes(pagina, tamanio, valle, estado)

    @GetMapping("/lotes/{id}")
    fun lote(@PathVariable id: Long) = agro.buscarLote(id).aRespuesta()

    @GetMapping("/lotes/{id}/labores")
    fun labores(@PathVariable id: Long, @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int) = agro.listarLabores(id, pagina, tamanio)

    @PostMapping("/labores")
    fun crear(@Valid @RequestBody datos: CrearLaborPeticion, auth: Authentication): ResponseEntity<LaborRespuesta> =
        ResponseEntity.status(HttpStatus.CREATED).body(agro.crear(datos, auth))

    @PutMapping("/labores/{id}")
    fun editar(@PathVariable id: Long, @Valid @RequestBody datos: EditarLaborPeticion, auth: Authentication) =
        agro.editar(id, datos, auth)

    @DeleteMapping("/labores/{id}")
    fun eliminar(@PathVariable id: Long): ResponseEntity<Void> {
        agro.eliminar(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/lotes/{id}/cerrar-campania")
    fun cerrar(@PathVariable id: Long) = agro.cerrarCampania(id)
}
