package com.uade.tpo.demo.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// Todos los errores salen con el mismo formato { status, message }, con un mensaje que el front puede mostrar:
// 400 regla de negocio o dato inválido, 401 sin sesión o login incorrecto, 403 sin permiso, 404 no existe.
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

    // El servidor de mail (Gmail) no aceptó el envío: credenciales mal, sin conexión, etc.
    // El pedido no se guarda (la transacción se deshace), así que se puede volver a intentar.
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMail(MailException ex) {
        log.error("No se pudo enviar un mail", ex);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "No pudimos enviar el email. Probá de nuevo en unos minutos.");
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
    // Un parámetro con un tipo que no corresponde (por ejemplo /bookings/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "El valor de \"" + ex.getName() + "\" no es válido");
    }

    // El cuerpo del pedido no es un JSON válido o no tiene el formato esperado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen un formato válido");
    }

    // Fotos más pesadas que el máximo permitido
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Las fotos pesan demasiado: cada una puede pesar hasta 5 MB");
    }

    // Cualquier otro error: los propios de Spring conservan su código con un mensaje en castellano;
    // los no previstos se loguean y responden 500 con un mensaje entendible.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return error(status, switch (status) {
                case NOT_FOUND -> "Esa dirección no existe en la API";
                case METHOD_NOT_ALLOWED -> "Ese método no está permitido en esta dirección";
                case UNSUPPORTED_MEDIA_TYPE -> "El formato de los datos enviados no es el esperado";
                case BAD_REQUEST -> ex instanceof MissingServletRequestParameterException missing
                        ? "Falta el dato \"" + missing.getParameterName() + "\""
                        : "La solicitud no es válida";
                default -> "No se pudo procesar la solicitud";
            });
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
