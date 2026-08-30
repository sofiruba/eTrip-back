package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.DiscountCouponRequestDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.DiscountCouponRepository;
import com.uade.tpo.demo.service.DiscountCouponService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscountCouponServiceImpl implements DiscountCouponService {

    private final DiscountCouponRepository discountCouponRepository;

    @Override
    public Page<DiscountCouponResponseDTO> getCoupons(Pageable pageable) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Consultar discountCouponRepository.findAll(pageable).
        // 2. Mapear cada DiscountCoupon a DiscountCouponResponseDTO.
        // 3. Retornar Page<DiscountCouponResponseDTO>.
        return null;
    }

    @Override
    public DiscountCouponResponseDTO getCouponById(Long couponId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar DiscountCoupon por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Mapear code, percentage, validFrom, validUntil y active.
        // 3. Retornar DiscountCouponResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public DiscountCouponResponseDTO createCoupon(DiscountCouponRequestDTO request) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que code no venga vacio y que percentage sea valido.
        // 2. Verificar que no exista otro cupon con el mismo code.
        // 3. Validar que validFrom sea anterior a validUntil.
        // 4. Instanciar DiscountCoupon con active true/false segun request.
        // 5. Guardar usando discountCouponRepository.save().
        // 6. Mapear y retornar DiscountCouponResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public DiscountCouponResponseDTO updateCoupon(Long couponId, DiscountCouponRequestDTO request)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar DiscountCoupon por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar que el nuevo code no duplique otro cupon.
        // 3. Actualizar percentage, fechas de vigencia y active.
        // 4. Guardar usando discountCouponRepository.save().
        // 5. Mapear y retornar DiscountCouponResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCoupon(Long couponId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar DiscountCoupon por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar si ya fue usado en alguna Order.
        // 3. Definir si corresponde borrado fisico o desactivarlo con active=false.
        // 4. Ejecutar la accion definida.
    }
}
