package pe.upeu.agrosierra.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.*
import java.time.Instant
import java.time.LocalDate

@Configuration
class DatosSemilla {
    @Bean
    fun inicializar(roles: RolRepository, usuarios: UsuarioRepository,
        lotes: LoteRepository, labores: LaborRepository, passwordEncoder: PasswordEncoder) = CommandLineRunner {
        if (roles.count() == 0L) {
            val comunes = mutableSetOf("LOTE_LEER", "LABOR_LEER")
            roles.saveAll(listOf(
                Rol(nombre = "TECNICO", privilegios = (comunes + setOf("LABOR_CREAR", "LABOR_EDITAR")).toMutableSet()),
                Rol(nombre = "JEFE_CAMPO", editaTodasLabores = true,
                    privilegios = (comunes + setOf("LABOR_CREAR", "LABOR_EDITAR", "LABOR_ELIMINAR", "CAMPANIA_CERRAR")).toMutableSet()),
                Rol(nombre = "ADMINISTRADOR", privilegios = (comunes + "USUARIO_GESTIONAR").toMutableSet())
            ))
        }
        // Alta idempotente: también se aplica si AgroAPI ya tiene una base H2 creada.
        if (roles.findByNombre("SUPERVISOR_CALIDAD") == null) {
            val lectura = mutableSetOf("LOTE_LEER", "LABOR_LEER", "LABOR_EDITAR")
            roles.save(
                Rol(
                    nombre = "SUPERVISOR_CALIDAD",
                    editaTodasLabores = true,
                    privilegios = lectura
                )
            )
        }
        if (usuarios.count() == 0L) {
            fun agregar(login: String, nombre: String, rol: String) = Usuario(
                usuario = login, nombre = nombre, passwordHash = passwordEncoder.encode("Agro2026*"),
                rol = roles.findByNombre(rol)!!)
            usuarios.saveAll(listOf(
                agregar("tecnico1", "Técnico de campo 1", "TECNICO"),
                agregar("jefe1", "Jefe de campo 1", "JEFE_CAMPO"),
                agregar("admin", "Administrador AgroSierra", "ADMINISTRADOR")
            ))
        }
        if (usuarios.findByUsuario("supervisor1") == null) {
            usuarios.save(
                Usuario(
                    usuario = "supervisor1",
                    nombre = "Supervisor de calidad",
                    passwordHash = passwordEncoder.encode("Agro2026*"),
                    rol = roles.findByNombre("SUPERVISOR_CALIDAD")!!
                )
            )
        }
        if (lotes.count() == 0L) {
            lotes.saveAll(listOf(
                Lote(codigo = "AG-001", nombre = "Chacra El Sol", hectareas = 4.0, cultivo = "Papa", valle = "Mantaro", campaniaCerrada = true, dosisMaximaPorHectarea = 2.0),
                Lote(codigo = "AG-002", nombre = "La Esperanza", hectareas = 3.5, cultivo = "Maíz", valle = "Cañete", campaniaCerrada = true, dosisMaximaPorHectarea = 1.5),
                Lote(codigo = "AG-003", nombre = "El Progreso", hectareas = 6.0, cultivo = "Quinua", valle = "Mantaro", dosisMaximaPorHectarea = 2.5),
                Lote(codigo = "AG-004", nombre = "Los Olivos", hectareas = 2.0, cultivo = "Palta", valle = "Majes", dosisMaximaPorHectarea = 1.0),
                Lote(codigo = "AG-005", nombre = "Pampa Verde", hectareas = 5.0, cultivo = "Arroz", valle = "Cañete", dosisMaximaPorHectarea = 2.0),
                Lote(codigo = "AG-006", nombre = "El Manantial", hectareas = 7.2, cultivo = "Uva", valle = "Majes", dosisMaximaPorHectarea = 3.0),
                Lote(codigo = "AG-007", nombre = "Santa Rosa", hectareas = 3.0, cultivo = "Papa", valle = "Mantaro", dosisMaximaPorHectarea = 1.5),
                Lote(codigo = "AG-008", nombre = "La Colina", hectareas = 4.5, cultivo = "Maíz", valle = "Cañete", dosisMaximaPorHectarea = 2.0)
            ))
        }
        if (labores.count() == 0L) {
            val loteLista = lotes.findAll().sortedBy { it.codigo }
            val tecnico = usuarios.findByUsuario("tecnico1")!!
            val jefe = usuarios.findByUsuario("jefe1")!!
            val base = LocalDate.now()
            val valores = mutableListOf<Labor>()
            loteLista.forEachIndexed { indice, lote ->
                // Dos labores por lote, incluso sobre los lotes inicialmente cerrados.
                valores += Labor(lote = lote, tipo = TipoLabor.RIEGO, fecha = base.minusDays(2),
                    autor = tecnico, cantidad = 1.0, cantidadJornales = 2,
                    observacion = "Riego programado", registradoEn = Instant.now().minusSeconds(7200))
                valores += Labor(lote = lote,
                    tipo = if (indice <= 2) TipoLabor.COSECHA else TipoLabor.FUMIGACION,
                    fecha = base.minusDays(1), autor = jefe, cantidad = 1.0,
                    cantidadJornales = if (indice <= 2) 4 else 3,
                    observacion = if (indice <= 2) "Cosecha registrada" else "Aplicación controlada",
                    registradoEn = Instant.now().minusSeconds(3600))
            }
            // Total de 20 labores; el lote AG-004 sigue sin COSECHA para demostrar RN-03.
            listOf(2, 2, 3, 3).forEachIndexed { i, indice ->
                valores += Labor(lote = loteLista[indice], tipo = TipoLabor.RIEGO,
                    fecha = base.minusDays(i.toLong()), autor = tecnico, cantidad = 2.0,
                    cantidadJornales = 2,
                    observacion = "Riego adicional ${i + 1}", registradoEn = Instant.now().minusSeconds(1800))
            }
            labores.saveAll(valores)
        }
    }
}
