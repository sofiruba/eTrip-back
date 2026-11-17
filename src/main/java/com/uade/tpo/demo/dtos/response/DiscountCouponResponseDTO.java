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
public class DiscountCouponResponseDTO {
    private Long id;
    private String code;
    private BigDecimal percentage;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
    /** Lo calcula el back: ACTIVE, INACTIVE, NOT_YET_VALID o EXPIRED. */
    private String status;
    /** En cuántas compras se usó (un cupón usado no se borra: se desactiva). */
    private long usedCount;
}
