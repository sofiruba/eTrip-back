package com.uade.tpo.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Moneda para VER los precios. En PLAN todo se publica y se cobra en pesos argentinos: otra moneda solo
 * convierte lo que se muestra, como referencia. La cotización la actualiza el admin desde el panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "currencies")
public class Currency {
    /** Código ISO: ARS, USD, EUR… */
    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    /** Cómo se escribe delante del monto: "US$", "€"… */
    @Column(nullable = false, length = 8)
    private String symbol;

    /** Cuántos pesos vale 1 unidad de esta moneda. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal arsPerUnit;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
