package com.uade.tpo.demo.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String role;

    /** Solo se completa si el que consulta es el mismo usuario o un ADMIN. */
    private String email;

    /** Cantidad de experiencias que publico (visible siempre). */
    private Long publishedExperiences;

    /** Reservas hechas por el usuario. Solo en el propio perfil o para ADMIN. */
    private Long bookingsCount;

    /** Reseñas escritas por el usuario. Solo en el propio perfil o para ADMIN. */
    private Long reviewsCount;
}
