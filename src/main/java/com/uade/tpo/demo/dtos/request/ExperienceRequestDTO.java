package com.uade.tpo.demo.dtos.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceRequestDTO {
    private String title;
    private String description;
    private BigDecimal price;
    private String location;
    private Long categoryId;
}
