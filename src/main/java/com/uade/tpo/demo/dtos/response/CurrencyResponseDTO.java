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
public class CurrencyResponseDTO {
    private String code;
    private String name;
    private String symbol;
    private BigDecimal arsPerUnit;
    private LocalDateTime updatedAt;
}
