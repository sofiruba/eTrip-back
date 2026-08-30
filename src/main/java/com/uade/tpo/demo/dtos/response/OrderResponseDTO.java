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
    private String couponCode;
    private LocalDateTime createdAt;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private List<BookingResponseDTO> bookings;
}
