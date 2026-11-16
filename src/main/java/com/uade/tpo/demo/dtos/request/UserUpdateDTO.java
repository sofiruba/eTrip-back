package com.uade.tpo.demo.dtos.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Campos editables del propio perfil. El email, la contraseña y el rol no se tocan aca.
 * Un campo que no viene (null) se deja como está; un texto vacío lo borra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {
    private String firstName;
    private String lastName;
    /** Presentación pública (hasta 500 caracteres). */
    private String bio;
    private String city;
    /** Foto como data URL o base64 (JPEG/PNG chico); "" la quita. */
    private String avatarBase64;
    /** Intereses (hasta 20). */
    private List<String> interests;
}
