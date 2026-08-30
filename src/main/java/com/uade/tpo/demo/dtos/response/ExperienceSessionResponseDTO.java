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
}
