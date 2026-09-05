package com.uade.tpo.demo.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "experiences")
public class Experience {
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

    @Transient
    public byte[] getCoverImage() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .min(Comparator.comparing(img -> img.getPosition() != null ? img.getPosition() : Integer.MAX_VALUE))
                .map(ExperienceImage::getImage)
                .orElse(null);
    }
}
