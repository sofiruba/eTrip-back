package com.uade.tpo.demo.dtos.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una experiencia guardada. El front ya tiene el catálogo: alcanza con el id. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDTO {
    private Long experienceId;
    private LocalDateTime createdAt;
}
