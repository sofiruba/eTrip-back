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
public class CurrencyRateRequestDTO {
    /** Cuántos pesos vale 1 unidad de la moneda. */
    private BigDecimal arsPerUnit;
}
