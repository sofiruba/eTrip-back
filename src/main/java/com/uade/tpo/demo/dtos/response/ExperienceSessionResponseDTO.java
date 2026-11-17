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
public class ExperienceSessionResponseDTO {
    private Long id;
    private Long experienceId;
    private String experienceTitle;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer capacity;
    private Integer availableSeats;
    private boolean active;
    /** PUBLISHED | SOLD_OUT | FINISHED | CANCELLED (lo calcula el back). */
    private String status;
    /** Si todavía se puede editar o cancelar (activa, sin terminar y futura). */
    private boolean editable;
    /** Personas anotadas para que les avisen si se libera un lugar. */
    private long waitlistCount;
    /** Hasta cuándo quien compre esta fecha la puede cancelar con reembolso (48 h antes de que empiece). */
    private LocalDateTime refundableUntil;
}
