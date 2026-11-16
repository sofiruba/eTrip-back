package com.uade.tpo.demo.controllers.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta del registro: todavía no hay token, primero hay que verificar el email con el código que le mandamos. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {

    /** A dónde mandamos el código. */
    private String email;
}
