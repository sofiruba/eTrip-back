package com.uade.tpo.demo.controllers.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.exceptions.GlobalExceptionHandler.ErrorResponse;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Errores que no pasan por ningún controller (por ejemplo, una dirección que no existe): responde con el mismo
 * formato { status, message } que GlobalExceptionHandler en vez de la página de error de Spring Boot.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.resolve(code instanceof Integer value ? value : 500);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = switch (status) {
            case NOT_FOUND -> "Esa dirección no existe en la API";
            case METHOD_NOT_ALLOWED -> "Ese método no está permitido en esta dirección";
            case UNAUTHORIZED -> "Tenés que iniciar sesión para hacer esto";
            case FORBIDDEN -> "No tenés permiso para hacer esto";
            default -> "Ocurrió un error inesperado. Probá de nuevo en unos minutos.";
        };
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}
