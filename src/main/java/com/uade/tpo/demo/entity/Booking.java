package com.uade.tpo.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "bookings")
public class Booking {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String voucherCode;

    @Column
    private Integer quantity;

    /**
     * Precio por persona que se pagó (con el descuento de la experiencia, sin el cupón). Se guarda al comprar
     * para que editar el precio después no cambie el voucher, los reembolsos ni las estadísticas.
     * Null en reservas viejas (anteriores a este campo): ahí se usa el precio actual.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean refunded = false;

    @Column
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_session_id", nullable = false)
    private ExperienceSession experienceSession;
}
