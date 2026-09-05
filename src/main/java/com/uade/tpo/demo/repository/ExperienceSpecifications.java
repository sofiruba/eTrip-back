package com.uade.tpo.demo.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Arma un {@link Specification} dinamico para buscar experiencias combinando
 * categoria, texto, ubicacion, rango de precio, vendedor (publisher) y rango de
 * fechas de sesion. Los filtros null no se aplican.
 */
public final class ExperienceSpecifications {

    private ExperienceSpecifications() {
    }

    public static Specification<Experience> withFilters(ExperienceSearchDTO f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), f.getCategoryId()));
            }
            if (f.getPublisherId() != null) {
                predicates.add(cb.equal(root.get("publisher").get("id"), f.getPublisherId()));
            }
            if (hasText(f.getTitle())) {
                predicates.add(cb.like(cb.lower(root.get("title")), contains(f.getTitle())));
            }
            if (hasText(f.getLocation())) {
                predicates.add(cb.like(cb.lower(root.get("location")), contains(f.getLocation())));
            }
            if (f.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.getMinPrice()));
            }
            if (f.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.getMaxPrice()));
            }
            if (Boolean.TRUE.equals(f.getOnlyDiscounted())) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("discountPercentage")),
                        cb.greaterThan(root.get("discountPercentage"), java.math.BigDecimal.ZERO)));
            }
            if (f.getDateFrom() != null || f.getDateTo() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ExperienceSession> session = sub.from(ExperienceSession.class);

                List<Predicate> sessionPredicates = new ArrayList<>();
                sessionPredicates.add(cb.equal(session.get("experience").get("id"), root.get("id")));
                if (f.getDateFrom() != null) {
                    sessionPredicates.add(cb.greaterThanOrEqualTo(session.get("startsAt"), f.getDateFrom()));
                }
                if (f.getDateTo() != null) {
                    sessionPredicates.add(cb.lessThanOrEqualTo(session.get("startsAt"), f.getDateTo()));
                }

                sub.select(cb.literal(1L)).where(sessionPredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
