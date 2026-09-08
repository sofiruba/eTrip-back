package com.uade.tpo.demo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class CategoryDuplicateException extends Exception {
    public CategoryDuplicateException() {
        super("La categoria que se intenta agregar esta duplicada");
    }

    public CategoryDuplicateException(String message) {
        super(message);
    }
}
