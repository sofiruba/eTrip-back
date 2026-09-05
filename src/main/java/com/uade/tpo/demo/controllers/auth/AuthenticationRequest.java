package com.uade.tpo.demo.controllers.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    /** Acepta el email o el username del usuario. */
    private String usernameOrEmail;
    String password;
}
