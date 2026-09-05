package com.uade.tpo.demo.dtos.response;

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
    private LocalDateTime createdAt;

    /** Quien hizo la reserva. Util sobre todo para la vista de vendedor (/bookings/sales). */
    private Long buyerId;
    private String buyerName;
}
