package com.uade.tpo.demo.service;

import java.util.List;

import com.uade.tpo.demo.dtos.response.NotificationResponseDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface NotificationService {
    List<NotificationResponseDTO> getMine(User user);

    NotificationResponseDTO markRead(Long notificationId, User user)
            throws ResourceNotFoundException, ForbiddenException;

    void create(User user, String title, String message);

    // Le avisa a quien compró (y a quien recibió el regalo, si ya se le entregó) que su reserva se canceló
    // por algo del anfitrión o de PLAN. why: "el anfitrión canceló la fecha", "el anfitrión dio de baja la experiencia"…
    void bookingCancelled(Booking booking, String why);
}
