package com.uade.tpo.demo.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "experience_sessions")
public class ExperienceSession {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Column
    private Integer capacity;

    @Column
    private Integer availableSeats;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_id", nullable = false)
    private Experience experience;

    @OneToMany(mappedBy = "experienceSession")
    private List<Booking> bookings;

    /** true si la fecha ya terminó. Se calcula con la hora en cada pedido (no hay ninguna tarea que la marque). */
    public boolean hasEnded() {
        return endsAt != null && endsAt.isBefore(LocalDateTime.now());
    }
}
