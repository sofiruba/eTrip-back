package com.uade.tpo.demo.dtos.request;

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
public class DiscountCouponRequestDTO {
    private String code;
    private BigDecimal percentage;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
}
