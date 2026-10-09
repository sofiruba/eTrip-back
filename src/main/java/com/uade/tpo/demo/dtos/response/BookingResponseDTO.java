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
    private LocalDateTime refundedAt;

    /** Quien hizo la reserva. Util sobre todo para la vista de vendedor (/bookings/sales). */
    private Long buyerId;
    private String buyerName;
}
