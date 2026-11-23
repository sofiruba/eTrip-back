package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calculadora de "Ser anfitrión" (GET /experiences/earnings-estimate). La cuenta la hace el back con la misma
 * comisión que se cobra de verdad en cada venta, así el front no repite ninguna regla de negocio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsEstimateDTO {
    /** Lo que pagan los huéspedes en una fecha (precio x lugares vendidos). */
    private BigDecimal grossPerSession;
    /** Comisión de PLAN en porcentaje (10 = 10 %). */
    private BigDecimal commissionRate;
    private BigDecimal commissionPerSession;
    /** Lo que le queda al anfitrión. */
    private BigDecimal netPerSession;
    private BigDecimal netPerMonth;
    private BigDecimal netPerYear;
    /** Hasta cuántas horas antes el huésped puede cancelar con reembolso (política de PLAN). */
    private int refundDeadlineHours;
    /** Precio promedio de las experiencias publicadas en la categoría elegida (null si no hay ninguna). */
    private BigDecimal suggestedPrice;
    private String categoryName;
    private long categoryExperienceCount;
}
