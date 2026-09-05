package com.uade.tpo.demo.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filtros opcionales para GET /experiences. Cualquier campo null se ignora.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceSearchDTO {
    private Long categoryId;
    private String title;
    private String location;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long publisherId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    /** true = solo experiencias con descuento activo (para la seccion "ofertas"). */
    private Boolean onlyDiscounted;
}
