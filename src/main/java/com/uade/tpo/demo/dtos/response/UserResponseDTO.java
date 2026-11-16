package com.uade.tpo.demo.dtos.response;

import java.util.List;

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
    private String username;
    private String firstName;
    private String lastName;
    /** Perfil público: presentación, ciudad, foto (base64 JPEG) e intereses. Los ven todos. */
    private String bio;
    private String city;
    private String avatarBase64;
    private List<String> interests;
    /** Rol y estado: solo en el propio perfil o para ADMIN (así no se puede buscar quién es admin recorriendo ids). */
    private String role;
    private Boolean active;

    /** Solo se completa si el que consulta es el mismo usuario o un ADMIN. */
    private String email;

    /** Cantidad de experiencias que publico (visible siempre). */
    private Long publishedExperiences;

    /** Reservas hechas por el usuario. Solo en el propio perfil o para ADMIN. */
    private Long bookingsCount;

    /** Reseñas escritas por el usuario. Solo en el propio perfil o para ADMIN. */
    private Long reviewsCount;
}
