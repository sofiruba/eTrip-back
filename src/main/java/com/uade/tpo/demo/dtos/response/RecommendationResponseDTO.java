package com.uade.tpo.demo.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una experiencia recomendada ("Para vos") y por qué se la recomendamos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDTO {
    private ExperienceResponseDTO experience;
    /** "Porque te gusta vinos y gastronomía", "Parecida a “…”, que guardaste"… */
    private String reason;
}
