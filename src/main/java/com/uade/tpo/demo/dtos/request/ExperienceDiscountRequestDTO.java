package com.uade.tpo.demo.dtos.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body de PATCH /experiences/{id}/discount. 0 o null = sacar el descuento. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDiscountRequestDTO {
    private BigDecimal discountPercentage;
}
