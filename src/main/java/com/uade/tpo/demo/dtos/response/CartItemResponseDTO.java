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
public class CartItemResponseDTO {
    private Long id;
    private Long experienceSessionId;
    private String experienceTitle;
    private Integer quantity;
    private BigDecimal unitPrice;
}
