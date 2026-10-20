package com.uade.tpo.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.User;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Arma un {@link Specification} dinamico para buscar experiencias: es toda la búsqueda del catálogo
 * (el front solo ordena). Los filtros null no se aplican.
 */
public final class ExperienceSpecifications {

    private ExperienceSpecifications() {
    }

    public static Specification<Experience> withFilters(ExperienceSearchDTO f, boolean includeInactive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeInactive) {
                // Catálogo público: solo publicadas
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (f.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), f.getCategoryId()));
            }
            if (f.getPublisherId() != null) {
                predicates.add(cb.equal(root.get("publisher").get("id"), f.getPublisherId()));
            }
            if (hasText(f.getLocation())) {
                predicates.add(cb.like(cb.lower(root.get("location")), contains(f.getLocation()), LIKE_ESCAPE));
            }
            // Texto: cada palabra tiene que estar en el título, el lugar o el nombre del anfitrión.
            // La collation de MySQL (utf8mb4_0900_ai_ci) ya no distingue mayúsculas ni acentos.
            if (hasText(f.getTitle())) {
                Join<Experience, User> publisher = root.join("publisher", JoinType.LEFT);
                Expression<String> searchable = cb.lower(cb.concat(cb.concat(cb.concat(
                        cb.concat(root.<String>get("title"), " "),
                        cb.concat(cb.coalesce(root.<String>get("location"), ""), " ")),
                        cb.concat(cb.coalesce(publisher.<String>get("firstName"), ""), " ")),
                        cb.coalesce(publisher.<String>get("lastName"), "")));
                for (String word : f.getTitle().trim().split("\\s+")) {
                    predicates.add(cb.like(searchable, contains(word), LIKE_ESCAPE));
                }
            }
            // Precio: el final, con el descuento aplicado (lo que paga el comprador)
            if (f.getMinPrice() != null || f.getMaxPrice() != null) {
                Expression<Number> finalPrice = finalPrice(root, cb);
                if (f.getMinPrice() != null) {
                    predicates.add(cb.ge(finalPrice, f.getMinPrice()));
                }
                if (f.getMaxPrice() != null) {
                    predicates.add(cb.le(finalPrice, f.getMaxPrice()));
                }
            }
            if (Boolean.TRUE.equals(f.getOnlyDiscounted())) {
                predicates.add(cb.greaterThan(root.get("discountPercentage"), BigDecimal.ZERO));
            }
            // Con niños o bebés en el grupo no se muestran las experiencias solo para mayores de 18
            if (Boolean.TRUE.equals(f.getWithKids())) {
                predicates.add(cb.isFalse(root.get("adultsOnly")));
            }
            // Fechas: el catálogo público muestra solo las que tienen alguna fecha vigente por delante,
            // y los filtros de fecha, personas, duración y horario tienen que cumplirse en UNA MISMA fecha
            if (!includeInactive || hasSessionFilters(f)) {
                predicates.add(cb.exists(matchingSession(f, root, query, cb)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Una fecha activa y futura de la experiencia que cumpla los filtros de fecha, lugares, duración y horario
    private static Subquery<Long> matchingSession(ExperienceSearchDTO f, Root<Experience> root, CriteriaQuery<?> query,
            CriteriaBuilder cb) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<ExperienceSession> session = sub.from(ExperienceSession.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(session.get("experience").get("id"), root.get("id")));
        predicates.add(cb.isTrue(session.get("active")));
        predicates.add(cb.greaterThan(session.get("startsAt"), LocalDateTime.now()));

        if (f.getDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(session.get("startsAt"), f.getDateFrom().atStartOfDay()));
        }
        if (f.getDateTo() != null) {
            predicates.add(cb.lessThan(session.get("startsAt"), f.getDateTo().plusDays(1).atStartOfDay()));
        }
        // Lugares: los que necesita el grupo; si se eligieron fechas o "solo con lugares", al menos uno
        int seats = f.getGuests() != null && f.getGuests() > 0 ? f.getGuests()
                : (f.getDateFrom() != null || f.getDateTo() != null || Boolean.TRUE.equals(f.getOnlyAvailable())) ? 1 : 0;
        if (seats > 0) {
            predicates.add(cb.greaterThanOrEqualTo(session.get("availableSeats"), seats));
        }
        // Duración en minutos (diferencia de segundos entre fin e inicio, con funciones de MySQL)
        if (f.getDurationMin() != null || f.getDurationMax() != null) {
            Expression<Long> seconds = cb.diff(
                    cb.function("UNIX_TIMESTAMP", Long.class, session.get("endsAt")),
                    cb.function("UNIX_TIMESTAMP", Long.class, session.get("startsAt")));
            if (f.getDurationMin() != null) {
                predicates.add(cb.ge(seconds, f.getDurationMin() * 60L));
            }
            if (f.getDurationMax() != null) {
                predicates.add(cb.le(seconds, f.getDurationMax() * 60L));
            }
        }
        // Franja horaria según la hora de inicio
        if (f.getTimeSlots() != null && !f.getTimeSlots().isEmpty()) {
            Expression<Integer> hour = cb.function("HOUR", Integer.class, session.get("startsAt"));
            List<Predicate> slots = new ArrayList<>();
            if (f.getTimeSlots().contains("manana")) {
                slots.add(cb.lt(hour, 12));
            }
            if (f.getTimeSlots().contains("tarde")) {
                slots.add(cb.and(cb.ge(hour, 12), cb.lt(hour, 17)));
            }
            if (f.getTimeSlots().contains("noche")) {
                slots.add(cb.ge(hour, 17));
            }
            if (!slots.isEmpty()) {
                predicates.add(cb.or(slots.toArray(new Predicate[0])));
            }
        }
        sub.select(cb.literal(1L)).where(predicates.toArray(new Predicate[0]));
        return sub;
    }

    private static boolean hasSessionFilters(ExperienceSearchDTO f) {
        return f.getDateFrom() != null || f.getDateTo() != null || f.getGuests() != null
                || Boolean.TRUE.equals(f.getOnlyAvailable()) || f.getDurationMin() != null || f.getDurationMax() != null
                || (f.getTimeSlots() != null && !f.getTimeSlots().isEmpty());
    }

    // price × (1 − descuento / 100); sin descuento, el precio de lista
    private static Expression<Number> finalPrice(Root<Experience> root, CriteriaBuilder cb) {
        Expression<BigDecimal> price = root.get("price");
        Expression<BigDecimal> discount = cb.coalesce(root.<BigDecimal>get("discountPercentage"), BigDecimal.ZERO);
        return cb.prod(price, cb.diff(BigDecimal.ONE, cb.quot(discount, BigDecimal.valueOf(100))));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Lo que escribe el usuario se busca literal: "%" o "_" no funcionan como comodines del LIKE
    private static final char LIKE_ESCAPE = '!';

    private static String contains(String value) {
        String literal = value.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + literal + "%";
    }
}
