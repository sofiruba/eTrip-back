package com.uade.tpo.demo.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filtros opcionales para GET /experiences. Cualquier campo null se ignora.
 * Los de fechas, personas, duración, horario y "solo con lugares" se cumplen en UNA MISMA fecha (sesión).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceSearchDTO {
    private Long categoryId;
    /** Cada palabra tiene que aparecer en el título, el lugar o el nombre del anfitrión. */
    private String title;
    private String location;
    /** Sobre el precio final (con el descuento aplicado). */
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long publisherId;
    /** true = solo experiencias con descuento activo (para la seccion "ofertas"). */
    private Boolean onlyDiscounted;

    /** Días (inclusive) en los que tiene que empezar la fecha. */
    private LocalDate dateFrom;
    private LocalDate dateTo;
    /** Lugares que necesita el grupo: la fecha tiene que tener al menos esos disponibles. */
    private Integer guests;
    /** true = solo con alguna fecha que tenga lugares. */
    private Boolean onlyAvailable;
    /** Duración de la fecha, en minutos. */
    private Integer durationMin;
    private Integer durationMax;
    /** Franjas de la hora de inicio: "manana" (antes de las 12), "tarde" (12 a 17), "noche" (desde las 17). */
    private List<String> timeSlots;
    /** true = el grupo tiene niños o bebés: no se muestran las experiencias solo para mayores de 18. */
    private Boolean withKids;
}
