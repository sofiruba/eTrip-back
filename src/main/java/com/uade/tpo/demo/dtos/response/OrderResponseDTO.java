package com.uade.tpo.demo.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private Long userId;
    /** PAID (pagada) | PARTIALLY_REFUNDED (se reembolsó alguna reserva) | CANCELLED (se reembolsaron todas). */
    private String status;
    private String couponCode;
    private LocalDateTime createdAt;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    /** Lo devuelto por reservas reembolsadas de esta orden. */
    private BigDecimal refunded;
    /** Comisión de PLAN (10% del total neto de reembolsos). */
    private BigDecimal commission;
    private List<BookingResponseDTO> bookings;
}
