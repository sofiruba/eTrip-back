package com.uade.tpo.demo.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceCategoryResponseDTO {
    private Long id;
    private String name;
    private String description;
    /** Cuántas experiencias (sin contar las dadas de baja) están en esta categoría. */
    private long experienceCount;
}
