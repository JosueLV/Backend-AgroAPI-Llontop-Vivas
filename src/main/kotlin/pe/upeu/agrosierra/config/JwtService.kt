package pe.upeu.agrosierra.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.agrosierra.dominio.Usuario
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(@Value("\${app.jwt.secret-base64}") claveBase64: String,
    @Value("\${app.jwt.expiration-minutes:60}") private val minutos: Long) {
    private val clave: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(claveBase64))

    fun crear(usuario: Usuario): String {
        val ahora = Instant.now()
        return Jwts.builder().subject(usuario.id.toString())
            .claim("usuario", usuario.usuario)
            .claim("privilegios", usuario.rol.privilegios.toList().sorted())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
            .signWith(clave).compact()
    }

    // verifyWith comprueba firma y parseSignedClaims verifica también la caducidad.
    fun validarYLeer(token: String): Claims = Jwts.parser().verifyWith(clave).build()
        .parseSignedClaims(token).payload
}
