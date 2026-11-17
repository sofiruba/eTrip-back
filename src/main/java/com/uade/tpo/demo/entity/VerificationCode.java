package com.uade.tpo.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Código pendiente que le mandamos al usuario por mail: para verificar su email o para recuperar la contraseña.
 * Un usuario tiene como mucho uno de cada tipo (user_id + purpose únicos): pedir otro reemplaza al anterior,
 * y se borra cuando se usa, vence o se agotan los intentos.
 * Se guarda el hash del código (nunca el código en texto), igual que la contraseña.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "verification_codes",
        uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "purpose" }))
public class VerificationCode {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodePurpose purpose;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Cuándo se mandó el mail: no mandamos otro hasta que pase un minuto
    @Column(nullable = false)
    private LocalDateTime sentAt;

    // Intentos fallidos con este código
    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;
}
