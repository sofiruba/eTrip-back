package com.uade.tpo.demo.dtos.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponseDTO {
    Long id;
    String title;
    String message;
    boolean read;
    LocalDateTime createdAt;
}
