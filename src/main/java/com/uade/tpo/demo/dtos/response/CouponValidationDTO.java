package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de GET /discount-coupons/validate. Siempre 200: si el cupon no sirve,
 * {@code valid = false} y {@code reason} explica por que (NOT_FOUND, INACTIVE, NOT_YET_VALID,
 * EXPIRED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationDTO {
    private String code;
    private boolean valid;
    private String reason;
    private BigDecimal percentage;
}
