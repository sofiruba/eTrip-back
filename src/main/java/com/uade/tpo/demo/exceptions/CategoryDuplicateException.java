package com.uade.tpo.demo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class CategoryDuplicateException extends Exception {
    public CategoryDuplicateException() {
        super("Ya existe una categoría con ese nombre");
    }

    public CategoryDuplicateException(String message) {
        super(message);
    }
}
