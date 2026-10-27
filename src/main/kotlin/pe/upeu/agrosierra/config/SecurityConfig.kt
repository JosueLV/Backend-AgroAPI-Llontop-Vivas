package pe.upeu.agrosierra.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity, filtro: JwtAuthenticationFilter, errores: EscritorError): SecurityFilterChain {
        http.csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/lotes/*/labores").hasAuthority("LABOR_LEER")
                    .requestMatchers(HttpMethod.GET, "/api/v1/lotes", "/api/v1/lotes/*").hasAuthority("LOTE_LEER")
                    .requestMatchers(HttpMethod.POST, "/api/v1/labores").hasAuthority("LABOR_CREAR")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/labores/*").hasAuthority("LABOR_EDITAR")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/labores/*").hasAuthority("LABOR_ELIMINAR")
                    .requestMatchers(HttpMethod.POST, "/api/v1/lotes/*/cerrar-campania").hasAuthority("CAMPANIA_CERRAR")
                    .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me").authenticated()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { req, res, _ ->
                    errores.responder(res, req, HttpStatus.UNAUTHORIZED, "Autenticación requerida")
                }
                it.accessDeniedHandler { req, res, _ ->
                    errores.responder(res, req, HttpStatus.FORBIDDEN, "No tienes el privilegio requerido")
                }
            }
            .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
