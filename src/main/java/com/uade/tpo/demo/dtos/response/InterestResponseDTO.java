package com.uade.tpo.demo.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestResponseDTO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    /** Cuántas experiencias del catálogo público, con alguna fecha con lugar, tienen este interés. */
    private long availableExperiences;
}
