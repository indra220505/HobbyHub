package com.hobbyhub.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.SQLException

data class ErrorResponse(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Business Logic Warning (IllegalArgumentException): {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Permintaan tidak valid."))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        log.warn("State Warning (IllegalStateException): {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Status tidak valid."))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("Validation Error: {}", errors)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = errors))
    }

    @ExceptionHandler(SQLException::class)
    fun handleDatabaseExceptions(ex: SQLException): ResponseEntity<ErrorResponse> {
        log.error("Database Error: ", ex)
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse(message = "Layanan database sedang tidak tersedia sementara."))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled Exception caught by GlobalExceptionHandler: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(message = ex.message ?: "Terjadi kesalahan internal pada server."))
    }
}
