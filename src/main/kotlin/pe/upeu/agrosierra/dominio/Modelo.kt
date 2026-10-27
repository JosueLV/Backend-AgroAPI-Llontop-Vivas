package pe.upeu.agrosierra.dominio

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "roles")
class Rol(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true, nullable = false) var nombre: String = "",
    // Configuración de alcance, sin comparar nombres de roles en los controladores.
    @Column(nullable = false) var editaTodasLabores: Boolean = false,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_privilegios", joinColumns = [JoinColumn(name = "rol_id")])
    @Column(name = "privilegio", nullable = false)
    var privilegios: MutableSet<String> = mutableSetOf()
)

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true, nullable = false) var usuario: String = "",
    @Column(nullable = false) var nombre: String = "",
    @Column(nullable = false) var passwordHash: String = "",
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "rol_id", nullable = false)
    var rol: Rol = Rol()
)

@Entity
@Table(name = "lotes")
class Lote(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true, nullable = false) var codigo: String = "",
    @Column(nullable = false) var nombre: String = "",
    @Column(nullable = false) var hectareas: Double = 0.0,
    @Column(nullable = false) var cultivo: String = "",
    @Column(nullable = false) var valle: String = "",
    @Column(nullable = false) var campaniaCerrada: Boolean = false,
    @Column(nullable = false) var dosisMaximaPorHectarea: Double = 0.0
)

enum class TipoLabor { RIEGO, FUMIGACION, COSECHA }

@Entity
@Table(name = "labores")
class Labor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lote_id", nullable = false)
    var lote: Lote = Lote(),
    @Enumerated(EnumType.STRING) @Column(nullable = false) var tipo: TipoLabor = TipoLabor.RIEGO,
    @Column(nullable = false) var fecha: LocalDate = LocalDate.now(),
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "usuario_id", nullable = false)
    var autor: Usuario = Usuario(),
    @Column(nullable = false) var cantidad: Double = 0.0,
    @Column(nullable = false, length = 500) var observacion: String = "",
    @Column(nullable = false) var registradoEn: Instant = Instant.now()
)
