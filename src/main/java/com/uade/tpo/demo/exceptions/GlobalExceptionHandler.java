package com.uade.tpo.demo.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// Intercepta las excepciones propias del proyecto para que el mensaje de cada
// "throw" (el que le pasamos nosotros al crear la excepcion) sea el que ve el
// cliente, en vez de un texto generico fijo por tipo de excepcion.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CategoryDuplicateException.class)
    public ResponseEntity<ErrorResponse> handleCategoryDuplicate(CategoryDuplicateException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "No tenés permiso para realizar esta acción");
    }

    // Login con datos incorrectos (401) o con una cuenta desactivada
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        String message = ex instanceof DisabledException
                ? "Tu cuenta está desactivada. Pedile a un administrador que la reactive."
                : "Usuario o contraseña incorrectos";
        return error(HttpStatus.UNAUTHORIZED, message);
    }

    // Un dato que la base no acepta (texto demasiado largo, un valor único repetido por dos pedidos a la vez):
    // es culpa del pedido, no del servidor.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Dato rechazado por la base: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.BAD_REQUEST, "Algún dato no es válido (muy largo o repetido). Revisalo y probá de nuevo.");
    }

    // Parámetros de paginación inválidos (?page=-1, ?size=0) y similares
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Parámetro inválido: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Algún parámetro de la solicitud no es válido");
    }

    // Cualquier error no previsto: se loguea y se responde con un mensaje entendible (nunca "No message available").
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Los errores propios de Spring (404 de ruta, body mal formado, etc.) mantienen su código
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return error(status, springError.getBody().getDetail() != null ? springError.getBody().getDetail() : status.getReasonPhrase());
        }
        if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
            return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido");
        }
        log.error("Error inesperado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Probá de nuevo en unos minutos.");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
    }
}
