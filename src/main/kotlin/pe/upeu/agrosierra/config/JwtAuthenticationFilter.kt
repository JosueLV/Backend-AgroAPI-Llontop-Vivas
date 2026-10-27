package pe.upeu.agrosierra.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import pe.upeu.agrosierra.repositorio.UsuarioRepository

@Component
class JwtAuthenticationFilter(private val jwt: JwtService,
    private val usuarios: UsuarioRepository, private val errores: EscritorError) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val cabecera = request.getHeader("Authorization")
        if (cabecera == null) {
            chain.doFilter(request, response)
            return
        }
        if (!cabecera.startsWith("Bearer ") || cabecera.length <= 7) {
            errores.responder(response, request, HttpStatus.UNAUTHORIZED, "Token ausente o inválido")
            return
        }
        try {
            val claims = jwt.validarYLeer(cabecera.substring(7))
            val id = claims.subject.toLong()
            val usuario = usuarios.findById(id).orElseThrow()
            if (usuario.usuario != claims["usuario"]) throw IllegalArgumentException("Token desactualizado")
            // Se obtienen privilegios actuales de la base de datos, no se confía ciegamente en un claim.
            val authorities = usuario.rol.privilegios.map(::SimpleGrantedAuthority)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(id.toString(), null, authorities)
        } catch (_: Exception) {
            SecurityContextHolder.clearContext()
            errores.responder(response, request, HttpStatus.UNAUTHORIZED, "Token inválido o vencido")
            return
        }
        chain.doFilter(request, response)
    }
}
