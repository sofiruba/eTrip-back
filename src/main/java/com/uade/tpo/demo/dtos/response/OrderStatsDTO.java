package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Estadísticas de ventas para el panel del ADMIN (GET /orders/stats). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {
    private long orderCount;
    /** Lo cobrado en todas las órdenes (ya con cupones aplicados). */
    private BigDecimal grossSales;
    /** Lo devuelto por reservas reembolsadas. */
    private BigDecimal refunded;
    /** Cobrado menos reembolsos. */
    private BigDecimal netSales;
    /** Comisión que se queda PLAN sobre las ventas netas. */
    private BigDecimal commission;
    /** Porcentaje de la comisión (10). */
    private BigDecimal commissionRate;
}
