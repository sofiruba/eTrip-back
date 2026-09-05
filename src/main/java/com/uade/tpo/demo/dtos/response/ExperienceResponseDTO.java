package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;
import java.util.List;

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

    /** Porcentaje de descuento aplicado (0-100), null si no tiene. */
    private BigDecimal discountPercentage;

    /** Precio ya con el descuento aplicado (igual a price si no hay descuento). */
    private BigDecimal finalPrice;

    private String location;

    /** Una o mas fotos en Base64, en el orden en que se cargaron. */
    private List<String> imagesBase64;

    private Long categoryId;
    private String categoryName;
    private Long publisherId;
    private String publisherName;
    private Double averageRating;
    private Integer reviewCount;
}
