package com.uade.tpo.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDateTime createdAt;

    @Column
    private BigDecimal subtotal;

    @Column
    private BigDecimal discountAmount;

    @Column
    private BigDecimal total;

    /** Compra como regalo: para quién es, su email (se le entrega cuando tenga una cuenta verificada con ese email) y una dedicatoria. */
    @Column(name = "gift_recipient_name", length = 100)
    private String giftRecipientName;

    @Column(name = "gift_recipient_email", length = 150)
    private String giftRecipientEmail;

    @Column(name = "gift_message", length = 300)
    private String giftMessage;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "discount_coupon_id")
    private DiscountCoupon discountCoupon;

    @OneToMany(mappedBy = "order")
    private List<Booking> bookings;
}
