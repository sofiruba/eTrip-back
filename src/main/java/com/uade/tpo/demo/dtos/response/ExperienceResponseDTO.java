package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceResponseDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String location;
    private String imageBase64;
    private Long categoryId;
    private String categoryName;
    private Long publisherId;
    private String publisherName;
    private Double averageRating;
    private Integer reviewCount;
}
