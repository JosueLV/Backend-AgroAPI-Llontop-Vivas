package pe.upeu.agrosierra

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class Aplicacion

fun main(args: Array<String>) {
    runApplication<Aplicacion>(*args)
}
