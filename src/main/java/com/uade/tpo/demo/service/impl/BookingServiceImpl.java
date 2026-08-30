package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.service.BookingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    public Page<BookingResponseDTO> getBookings(Pageable pageable) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Consultar bookingRepository.findAll(pageable).
        // 2. Mapear cada Booking a BookingResponseDTO.
        // 3. Incluir datos de Order y ExperienceSession necesarios para mostrar el voucher.
        // 4. Retornar Page<BookingResponseDTO>.
        return null;
    }

    @Override
    public BookingResponseDTO getBookingById(Long bookingId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar Booking por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar permisos: usuario dueno de la orden o ADMIN.
        // 3. Mapear voucherCode, quantity, createdAt, orderId y datos de ExperienceSession.
        // 4. Retornar BookingResponseDTO.
        return null;
    }
}
