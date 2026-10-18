package com.uade.tpo.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.DiscountCouponRequestDTO;
import com.uade.tpo.demo.dtos.response.CouponValidationDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.entity.DiscountCoupon;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.CartRepository;
import com.uade.tpo.demo.repository.DiscountCouponRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.service.DiscountCouponService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscountCouponServiceImpl implements DiscountCouponService {

    private final DiscountCouponRepository discountCouponRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    // Lista todos los cupones (activos e inactivos).
    @Override
    @Transactional(readOnly = true)
    public Page<DiscountCouponResponseDTO> getCoupons(Pageable pageable) {
        return discountCouponRepository.findAll(pageable).map(this::toResponse);
    }

    // Un cupón puntual por id.
    @Override
    @Transactional(readOnly = true)
    public DiscountCouponResponseDTO getCouponById(Long couponId) throws ResourceNotFoundException {
        return toResponse(discountCouponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un cupon con id " + couponId)));
    }

    static final String ALREADY_USED = "ALREADY_USED";

    // Chequea si un código es aplicable hoy (existe, activo, vigente) y si este usuario todavía no lo usó;
    // no lo gasta ni lo modifica. Es el mismo criterio que se aplica al confirmar la compra.
    @Override
    @Transactional(readOnly = true)
    public CouponValidationDTO validateCoupon(String code, User user) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        DiscountCoupon coupon = discountCouponRepository.findByCode(normalized).orElse(null);

        String reason = reasonIfInvalid(coupon);
        if (reason == null && user != null && orderRepository.existsByUserIdAndDiscountCouponId(user.getId(), coupon.getId())) {
            reason = ALREADY_USED;
        }
        // Montos con el carrito actual, para que el checkout muestre lo mismo que va a cobrar la orden
        BigDecimal subtotal = cartSubtotal(user);
        BigDecimal discount = reason == null
                ? subtotal.multiply(coupon.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return CouponValidationDTO.builder()
                .code(normalized)
                .valid(reason == null)
                .reason(reason)
                .message(reason != null ? describeReason(reason) : null)
                .percentage(coupon != null ? coupon.getPercentage() : null)
                .subtotal(subtotal)
                .discountAmount(discount)
                .total(subtotal.subtract(discount))
                .build();
    }

    // Suma del carrito del usuario con los precios vigentes (mismo cálculo que la orden).
    private BigDecimal cartSubtotal(User user) {
        if (user == null) {
            return BigDecimal.ZERO;
        }
        return cartRepository.findByUserId(user.getId())
                .map(cart -> cart.getItems() == null ? BigDecimal.ZERO : cart.getItems().stream()
                        .map(item -> {
                            BigDecimal price = item.getExperienceSession().getExperience().getEffectivePrice();
                            return (price != null ? price : BigDecimal.ZERO).multiply(BigDecimal.valueOf(item.getQuantity()));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);
    }

    // Texto para el usuario de cada código de motivo (lo usa la creación de la orden).
    static String describeReason(String reason) {
        return switch (reason) {
            case "NOT_FOUND" -> "No existe un cupón con ese código";
            case "INACTIVE" -> "Este cupón ya no está activo";
            case "INVALID_PERCENTAGE" -> "Este cupón no tiene un descuento válido";
            case "NOT_YET_VALID" -> "Este cupón todavía no está vigente";
            case "EXPIRED" -> "Este cupón venció";
            case ALREADY_USED -> "Ya usaste este cupón en otra compra";
            default -> "El cupón no es válido";
        };
    }

    // Devuelve null si el cupón es aplicable hoy, o un código de motivo si no.
    static String reasonIfInvalid(DiscountCoupon coupon) {
        if (coupon == null) {
            return "NOT_FOUND";
        }
        if (coupon.getActive() == null || !coupon.getActive()) {
            return "INACTIVE";
        }
        if (coupon.getPercentage() == null || coupon.getPercentage().signum() <= 0) {
            return "INVALID_PERCENTAGE";
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            return "NOT_YET_VALID";
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            return "EXPIRED";
        }
        return null;
    }

    // Crea un cupón nuevo (código único, porcentaje entre 0 y 100, rango de vigencia válido).
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public DiscountCouponResponseDTO createCoupon(DiscountCouponRequestDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException("Faltan los datos del cupón");
        }

        // Valida que el código no exista ya (no se permite duplicado).
        String code = requireCode(request.getCode());
        if (discountCouponRepository.findByCode(code).isPresent()) {
            throw new BadRequestException("Ya existe un cupón con ese código");
        }

        // Valida que el porcentaje sea positivo y menor o igual a 100, y que el rango de vigencia sea válido.
        BigDecimal percentage = requirePercentage(request.getPercentage());
        validateDateRange(request.getValidFrom(), request.getValidUntil());

        DiscountCoupon coupon = DiscountCoupon.builder()
                .code(code)
                .percentage(percentage)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(request.getActive() != null ? request.getActive() : Boolean.TRUE)
                .build();

        return toResponse(discountCouponRepository.save(coupon));
    }

    // Actualiza los campos que vengan informados de un cupón existente.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public DiscountCouponResponseDTO updateCoupon(Long couponId, DiscountCouponRequestDTO request)
            throws ResourceNotFoundException, BadRequestException {
                // Busca el cupón existente; si no existe, lanza excepción.
        DiscountCoupon coupon = discountCouponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("El cupón no existe"));
        if (request == null) {
            throw new BadRequestException("Faltan los datos del cupón");
        }

        // Valida y actualiza los campos que vengan informados en el request.
        if (request.getCode() != null) {
            String code = requireCode(request.getCode());
            Optional<DiscountCoupon> sameCode = discountCouponRepository.findByCode(code);
            if (sameCode.isPresent() && !sameCode.get().getId().equals(couponId)) {
                throw new BadRequestException("Ya existe un cupón con ese código");
            }
            coupon.setCode(code);
        }

        // Valida y actualiza el porcentaje, rango de vigencia y estado activo si vienen informados.
        if (request.getPercentage() != null) {
            coupon.setPercentage(requirePercentage(request.getPercentage()));
        }
        if (request.getValidFrom() != null) {
            coupon.setValidFrom(request.getValidFrom());
        }
        if (request.getValidUntil() != null) {
            coupon.setValidUntil(request.getValidUntil());
        }
        validateDateRange(coupon.getValidFrom(), coupon.getValidUntil());

        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        return toResponse(discountCouponRepository.save(coupon));
    }

    // Borra el cupón si nunca se usó; si ya se usó en una orden, lo desactiva en vez de
    // borrarlo, para no perder la integridad histórica de esas órdenes.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCoupon(Long couponId) throws ResourceNotFoundException {
        DiscountCoupon coupon = discountCouponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("El cupón no existe"));

        if (orderRepository.existsByDiscountCouponId(couponId)) {
            coupon.setActive(false);
            discountCouponRepository.save(coupon);
            return;
        }

        discountCouponRepository.delete(coupon);
    }

    private String requireCode(String code) throws BadRequestException {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("El cupón necesita un código");
        }
        return code.trim().toUpperCase();
    }

    private BigDecimal requirePercentage(BigDecimal percentage) throws BadRequestException {
        if (percentage == null
                || percentage.signum() <= 0
                || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El descuento tiene que ser mayor a 0 y hasta 100%");
        }
        return percentage;
    }

    private void validateDateRange(LocalDateTime validFrom, LocalDateTime validUntil) throws BadRequestException {
        if (validFrom != null && validUntil != null && !validFrom.isBefore(validUntil)) {
            throw new BadRequestException("La fecha de fin tiene que ser posterior a la de inicio");
        }
    }

    private DiscountCouponResponseDTO toResponse(DiscountCoupon coupon) {
        return DiscountCouponResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .percentage(coupon.getPercentage())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .active(coupon.getActive())
                .build();
    }
}
