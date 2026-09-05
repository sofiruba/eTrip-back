package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Long experienceId;
    private String experienceTitle;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer quantity;

    /** Precio unitario ya con el descuento del producto aplicado (Experience.getEffectivePrice()). */
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
