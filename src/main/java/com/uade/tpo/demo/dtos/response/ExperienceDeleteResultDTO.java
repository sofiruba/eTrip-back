package com.uade.tpo.demo.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Qué pasó al eliminar una experiencia: cuántas fechas futuras se cancelaron, cuántas reservas se reembolsaron
 * y si se borró de la base (nunca se vendió) o quedó guardada para el historial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDeleteResultDTO {
    private Long experienceId;
    private boolean removed;
    private int cancelledSessions;
    private int refundedBookings;
}
