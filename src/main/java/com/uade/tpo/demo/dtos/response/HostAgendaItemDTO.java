package com.uade.tpo.demo.dtos.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen de una experiencia del anfitrión para "Fechas y huéspedes": cuántas fechas tiene en cada estado
 * y cómo vienen las próximas (lugares reservados, ocupación y lista de espera). Lo calcula el back.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostAgendaItemDTO {
    private Long experienceId;
    private String experienceTitle;
    /** false si está pausada (no aparece en el catálogo). */
    private boolean experienceActive;
    /** Inicio de la próxima fecha vigente (null si no tiene). */
    private LocalDateTime nextSessionAt;
    /** Fechas por estado, igual que el status de cada fecha: próximas (PUBLISHED o SOLD_OUT), FINISHED y CANCELLED. */
    private int upcomingSessions;
    private int finishedSessions;
    private int cancelledSessions;
    /** Solo las próximas: lugares reservados, lugares en total y porcentaje ocupado. */
    private int bookedSeats;
    private int capacity;
    private int occupancyPercent;
    /** Personas en lista de espera en las próximas fechas. */
    private long waitlistCount;
}
