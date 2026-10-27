package pe.upeu.agrosierra.seguridad

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.config.JwtService
import pe.upeu.agrosierra.repositorio.UsuarioRepository
import pe.upeu.agrosierra.servicio.AgroServicio
import pe.upeu.agrosierra.servicio.ErrorNegocio
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1")
class AuthController(private val usuarios: UsuarioRepository, private val passwords: PasswordEncoder,
    private val jwt: JwtService, private val agro: AgroServicio) {

    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody datos: LoginPeticion): LoginRespuesta {
        val usuario = usuarios.findByUsuario(datos.usuario)
        if (usuario == null || !passwords.matches(datos.contrasena, usuario.passwordHash)) {
            throw ErrorNegocio(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos")
        }
        return LoginRespuesta(jwt.crear(usuario), usuario = usuario.usuario, nombre = usuario.nombre,
            rol = usuario.rol.nombre, privilegios = usuario.rol.privilegios.toSortedSet())
    }

    @GetMapping("/usuarios/me")
    fun me(auth: Authentication): UsuarioRespuesta = agro.usuarioActual(auth).aRespuesta()
}
