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
public class BookingResponseDTO {
    private Long id;
    private String voucherCode;
    private Long orderId;
    private Long experienceSessionId;
    private Long experienceId;
    private String experienceTitle;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
    private boolean refunded;
    /** CONFIRMED | COMPLETED (la fecha ya terminó) | REFUNDED (lo calcula el back). */
    private String status;
    private LocalDateTime refundedAt;

    /**
     * Plata de la reserva (en 0 si se reembolsó): lo que pagó el comprador (con el cupón repartido),
     * la comisión de PLAN y lo que le queda al anfitrión. La comisión está incluida en el precio.
     */
    private BigDecimal paidAmount;
    private BigDecimal commission;
    private BigDecimal hostEarnings;

    /** Quien hizo la reserva. Util sobre todo para la vista de vendedor (/bookings/sales). */
    private Long buyerId;
    private String buyerName;
}
