package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.CartRepository;
import com.uade.tpo.demo.repository.DiscountCouponRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final DiscountCouponRepository discountCouponRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    public Page<OrderResponseDTO> getOrders(Pageable pageable) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Consultar orderRepository.findAll(pageable).
        // 2. Mapear cada Order a OrderResponseDTO.
        // 3. Incluir Bookings asociados como BookingResponseDTO.
        // 4. Retornar Page<OrderResponseDTO>.
        return null;
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar Order por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Mapear datos de usuario, cupon, totales y bookings.
        // 3. Retornar OrderResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO createOrder(OrderRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar el User en la BD usando request.getUserId(). Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar el Cart del usuario usando cartRepository.findByUserId(request.getUserId()).
        // 3. Validar que el Cart exista y no este vacio.
        // 4. Recorrer los CartItems y validar cupos disponibles en cada ExperienceSession.
        // 5. Calcular subtotal sumando precio de experiencia * cantidad de cada item.
        // 6. Si request.getCouponCode() viene informado, buscar DiscountCoupon por code.
        // 7. Validar que el cupon este activo, vigente y aplique a la compra.
        // 8. Calcular discountAmount y total.
        // 9. Instanciar Order asociando User y DiscountCoupon opcional.
        // 10. Guardar Order usando orderRepository.save().
        // 11. Por cada CartItem, crear un Booking asociado a la Order y a la ExperienceSession.
        // 12. Generar voucherCode para cada Booking segun regla del equipo.
        // 13. Descontar availableSeats de cada ExperienceSession.
        // 14. Guardar Bookings usando bookingRepository.save().
        // 15. Vaciar el Cart del usuario eliminando sus CartItems.
        // 16. Retornar OrderResponseDTO con sus BookingResponseDTO.
        return null;
    }
}
