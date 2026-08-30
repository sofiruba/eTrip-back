package com.uade.tpo.demo.dtos.request;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceSessionRequestDTO {
    private Long experienceId;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer capacity;
}
