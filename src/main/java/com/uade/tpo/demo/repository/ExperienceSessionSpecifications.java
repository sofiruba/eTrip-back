package com.uade.tpo.demo.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.uade.tpo.demo.entity.ExperienceSession;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros combinables para GET /experience-sessions. Los que vienen null no se
 * aplican.
 */
public final class ExperienceSessionSpecifications {

    private ExperienceSessionSpecifications() {
    }

    public static Specification<ExperienceSession> withFilters(
            Long experienceId, Boolean onlyAvailable, LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (experienceId != null) {
                predicates.add(cb.equal(root.get("experience").get("id"), experienceId));
            }
            if (Boolean.TRUE.equals(onlyAvailable)) {
                predicates.add(cb.greaterThan(root.get("availableSeats"), 0));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startsAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
