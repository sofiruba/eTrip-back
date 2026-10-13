package com.uade.tpo.demo.controllers.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgotPasswordResponse {

    /** Solo en desarrollo (app.password-reset.show-code=true): el código, para verlo en la consola del navegador. */
    private String devCode;
}
