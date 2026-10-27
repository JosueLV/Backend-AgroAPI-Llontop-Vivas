package pe.upeu.agrosierra

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.repositorio.LaborRepository
import java.time.Instant

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:agrotest;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
@AutoConfigureMockMvc
@Transactional
class AgroApiIntegrationTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: ObjectMapper
    @Autowired lateinit var labores: LaborRepository

    private fun token(usuario: String): String {
        val respuesta = mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"usuario":"$usuario","contrasena":"Agro2026*"}"""))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return json.readTree(respuesta)["token"].asText()
    }

    private fun cuerpoLote(lote: Long, tipo: String = "RIEGO", cantidad: Double = 1.0) =
        """{"loteId":$lote,"tipo":"$tipo","fecha":"2026-10-27","cantidad":$cantidad,"observacion":"Prueba"}"""

    @Test fun `el login devuelve un JWT firmado y privilegios del rol`() {
        val respuesta = mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"usuario":"tecnico1","contrasena":"Agro2026*"}"""))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val datos = json.readTree(respuesta)
        assertEquals("TECNICO", datos["rol"].asText())
        assertTrue(datos["token"].asText().split('.').size == 3)
        assertTrue(datos["privilegios"].any { it.asText() == "LABOR_CREAR" })
    }

    @Test fun `sin token recibe 401 y token manipulado recibe 401`() {
        mvc.perform(get("/api/v1/lotes")).andExpect(status().isUnauthorized)
        val cambiado = token("tecnico1").dropLast(2) + "xx"
        mvc.perform(get("/api/v1/lotes").header("Authorization", "Bearer $cambiado"))
            .andExpect(status().isUnauthorized)
    }

    @Test fun `el administrador no puede crear una labor`() {
        mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer ${token("admin")}")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpoLote(3)))
            .andExpect(status().isForbidden)
    }

    @Test fun `crear en lote cerrado da 409`() {
        mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer ${token("tecnico1")}")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpoLote(1)))
            .andExpect(status().isConflict)
    }

    @Test fun `fumigacion sobre dosis maxima da 400 por campo`() {
        mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer ${token("tecnico1")}")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpoLote(4, "FUMIGACION", 999.0)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.validationErrors.cantidad").exists())
    }

    @Test fun `sin cosecha el jefe no puede cerrar campania`() {
        mvc.perform(post("/api/v1/lotes/4/cerrar-campania")
            .header("Authorization", "Bearer ${token("jefe1")}"))
            .andExpect(status().isConflict)
    }

    @Test fun `paginacion y filtros tienen la envoltura requerida`() {
        mvc.perform(get("/api/v1/lotes?pagina=0&tamanio=2&estado=ABIERTA&valle=Majes")
            .header("Authorization", "Bearer ${token("tecnico1")}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contenido.length()").value(2))
            .andExpect(jsonPath("$.totalElementos").value(2))
            .andExpect(jsonPath("$.ultima").value(true))
    }

    @Test fun `tecnico no puede corregir labor de otro`() {
        // Labor 8 = fumigación de AG-004 registrada por jefe1; AG-004 está abierto.
        mvc.perform(put("/api/v1/labores/8").header("Authorization", "Bearer ${token("tecnico1")}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"tipo":"RIEGO","fecha":"2026-10-27","cantidad":1.0,"observacion":"Cambio"}"""))
            .andExpect(status().isForbidden)
    }

    @Test fun `tecnico no puede corregir su labor pasada la ventana de 24 horas`() {
        val labor = labores.findById(7).orElseThrow() // AG-004 abierta, creada por tecnico1
        labor.registradoEn = Instant.now().minusSeconds(25 * 3600)
        labores.saveAndFlush(labor)
        mvc.perform(put("/api/v1/labores/7").header("Authorization", "Bearer ${token("tecnico1")}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"tipo":"RIEGO","fecha":"2026-10-27","cantidad":1.0,"observacion":"Tarde"}"""))
            .andExpect(status().isForbidden)
    }

    @Test fun `jefe puede cerrar campania despues de una cosecha`() {
        val jefe = token("jefe1")
        mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer $jefe")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpoLote(4, "COSECHA")))
            .andExpect(status().isCreated)
        mvc.perform(post("/api/v1/lotes/4/cerrar-campania").header("Authorization", "Bearer $jefe"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.campaniaCerrada").value(true))
    }

    @Test fun `crear obtiene id positivo y borrar devuelve 204 sin cuerpo`() {
        val jefeToken = token("jefe1")
        val respuesta = mvc.perform(post("/api/v1/labores")
            .header("Authorization", "Bearer $jefeToken")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpoLote(4)))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val id = json.readTree(respuesta)["id"].asLong()
        assertTrue(id > 0)
        val borrado = mvc.perform(delete("/api/v1/labores/$id")
            .header("Authorization", "Bearer $jefeToken"))
            .andExpect(status().isNoContent).andReturn().response
        assertEquals("", borrado.contentAsString)
    }

    @Test fun `usuario actual devuelve los privilegios`() {
        mvc.perform(get("/api/v1/usuarios/me")
            .header("Authorization", "Bearer ${token("jefe1")}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rol").value("JEFE_CAMPO"))
    }
}
