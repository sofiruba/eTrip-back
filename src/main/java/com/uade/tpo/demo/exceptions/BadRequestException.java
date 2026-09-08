package com.uade.tpo.demo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class BadRequestException extends Exception {
    public BadRequestException() {
        super("Los datos enviados no son validos");
    }

    public BadRequestException(String message) {
        super(message);
    }
}
