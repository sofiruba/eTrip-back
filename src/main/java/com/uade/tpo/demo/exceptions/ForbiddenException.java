package com.uade.tpo.demo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class ForbiddenException extends Exception {
    public ForbiddenException() {
        super("No tenes permiso para realizar esta accion");
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
