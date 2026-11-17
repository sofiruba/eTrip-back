package com.uade.tpo.demo.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "experiences")
public class Experience {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column
    private BigDecimal price;

    @Column
    private String location;

    /** Descuento sobre el precio de lista, en porcentaje (0-100). Null/0 = sin descuento. */
    @Column(name = "discount_percentage")
    private BigDecimal discountPercentage;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Solo para mayores de 18: con niños o bebés en la búsqueda no se muestra. */
    @Column(nullable = false)
    @Builder.Default
    private boolean adultsOnly = false;

    /**
     * Cuándo la eliminó su anfitrión. Una experiencia que alguna vez se vendió no se borra de la base (los compradores
     * conservan sus vouchers y las estadísticas dependen de sus reservas): queda con esta fecha, pausada y oculta.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Intereses con los que el anfitrión etiqueta la experiencia (hasta 8): son los mismos que eligen los usuarios. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "experience_interest",
            joinColumns = @JoinColumn(name = "experience_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    @OrderBy("name ASC")
    private Set<Interest> tags;

    @OneToMany(mappedBy = "experience", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ExperienceImage> images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExperienceCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private User publisher;

    @OneToMany(mappedBy = "experience")
    private List<Review> reviews;

    @OneToMany(mappedBy = "experience")
    private List<ExperienceSession> sessions;

    /** Precio final tras aplicar {@link #discountPercentage} (si tiene). No se persiste. */
    @Transient
    public BigDecimal getEffectivePrice() {
        if (price == null) {
            return null;
        }
        if (discountPercentage == null || discountPercentage.signum() <= 0) {
            return price;
        }
        BigDecimal factor = BigDecimal.valueOf(100).subtract(discountPercentage);
        return price.multiply(factor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
