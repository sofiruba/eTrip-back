package com.uade.tpo.demo.controllers.auth;

import java.util.List;

import com.uade.tpo.demo.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private Role role;
    /** Intereses elegidos en el registro (opcional, hasta 20). */
    private List<String> interests;
}
