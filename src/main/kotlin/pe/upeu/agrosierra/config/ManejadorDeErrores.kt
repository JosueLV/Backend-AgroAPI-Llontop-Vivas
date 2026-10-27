package pe.upeu.agrosierra.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import pe.upeu.agrosierra.servicio.ErrorNegocio
import java.time.LocalDateTime

data class ErrorApi(val timestamp: String = LocalDateTime.now().toString(), val status: Int,
    val error: String, val message: String, val path: String,
    val validationErrors: Map<String, String> = emptyMap())

fun cuerpo(status: HttpStatus, mensaje: String, ruta: String, campos: Map<String, String> = emptyMap()) =
    ErrorApi(status = status.value(), error = status.reasonPhrase, message = mensaje, path = ruta, validationErrors = campos)

@Component
class EscritorError(private val mapper: ObjectMapper) {
    fun responder(response: HttpServletResponse, request: HttpServletRequest, status: HttpStatus, mensaje: String) {
        response.status = status.value()
        response.contentType = "application/json;charset=UTF-8"
        mapper.writeValue(response.outputStream, cuerpo(status, mensaje, request.requestURI))
    }
}

@RestControllerAdvice
class ManejadorDeErrores {
    @ExceptionHandler(ErrorNegocio::class)
    fun regla(ex: ErrorNegocio, request: HttpServletRequest): ResponseEntity<ErrorApi> =
        ResponseEntity.status(ex.estado).body(cuerpo(ex.estado, ex.message ?: "Error", request.requestURI, ex.campos))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorApi> {
        val campos = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") }
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Datos inválidos", request.requestURI, campos))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoInvalido(ex: HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<ErrorApi> =
        ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "JSON, fecha o tipo de labor inválido", request.requestURI))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ErrorApi> =
        ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Parámetro inválido",
            request.requestURI, mapOf(ex.name to "El valor no tiene el tipo esperado")))

    @ExceptionHandler(AccessDeniedException::class)
    fun prohibido(ex: AccessDeniedException, request: HttpServletRequest): ResponseEntity<ErrorApi> =
        ResponseEntity.status(403).body(cuerpo(HttpStatus.FORBIDDEN, "No tienes el privilegio requerido", request.requestURI))

    @ExceptionHandler(Exception::class)
    fun inesperado(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorApi> =
        ResponseEntity.status(500).body(cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servicio", request.requestURI))
}
