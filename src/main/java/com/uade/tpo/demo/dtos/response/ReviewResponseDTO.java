package com.uade.tpo.demo.dtos.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private Long id;
    private Long experienceId;
    private String experienceTitle;
    private Long userId;
    private String userName;
    // Foto de perfil del autor (en base64, igual que en UserResponseDTO); null si no subió ninguna
    private String userAvatarBase64;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Long hostId;
    private String hostName;
    private String hostReply;
    private LocalDateTime hostReplyAt;
    // Solo al crear o borrar: cómo quedó la experiencia (el front actualiza su promedio sin volver a pedirla)
    private Double experienceAverageRating;
    private Long experienceReviewCount;
}
