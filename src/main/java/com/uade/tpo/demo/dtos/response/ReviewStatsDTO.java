package com.uade.tpo.demo.dtos.response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen de las reseñas de una experiencia (GET /reviews/experience/{id}/stats). Las reseñas llegan paginadas,
 * así que el promedio y las barras por puntaje los calcula el back sobre todas, no el front sobre una página.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsDTO {
    private Long experienceId;
    private long count;
    private double average;
    /** Cuántas reseñas hay de cada puntaje: { 5: 12, 4: 3, 3: 0, 2: 1, 1: 0 }. */
    private Map<Integer, Long> distribution;
}
