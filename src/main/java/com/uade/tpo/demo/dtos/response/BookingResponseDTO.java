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
    private String experienceTitle;
    private Integer quantity;
    private LocalDateTime createdAt;
}
