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
     * Plata de la reserva (en 0 si se canceló): lo que pagó el comprador (con el cupón repartido), lo que recibe
     * el anfitrión (precio de lista menos el 10 %: los cupones los absorbe PLAN) y lo que le queda a PLAN.
     */
    private BigDecimal paidAmount;
    private BigDecimal commission;
    private BigDecimal hostEarnings;

    /** Quien hizo la reserva (lo usa sobre todo el anfitrión en /bookings/sales). */
    private Long buyerId;
    private String buyerName;
    /** Si la compra fue un regalo: para quién, la dedicatoria y quién lo regala. */
    private String giftRecipientName;
    private String giftMessage;
    private String giftFromName;
    /** Si es un regalo: PENDING (esperando que quien lo recibe tenga cuenta verificada con ese email), DELIVERED (ya es suyo) o IN_PERSON (sin email: se entrega en mano). */
    private String giftStatus;
    /** El usuario al que se le entregó el regalo. */
    private Long recipientId;

    /** Si quien la mira la puede cancelar con reembolso ahora, y hasta cuándo (null si no puede). */
    private boolean refundable;
    private LocalDateTime refundableUntil;

    /** Si quien la mira la puede reseñar: tiene el voucher, no se canceló y la experiencia ya terminó. */
    private boolean reviewable;
}
