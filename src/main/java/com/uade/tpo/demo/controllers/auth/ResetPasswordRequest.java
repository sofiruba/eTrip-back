package com.uade.tpo.demo.controllers.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {

    /** Acepta el email o el username del usuario. */
    private String usernameOrEmail;
    private String code;
    /** Solo lo usa /reset-password; /verify-reset-code lo ignora. */
    private String newPassword;
}
