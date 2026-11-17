package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.response.NotificationResponseDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Notification;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.NotificationRepository;
import com.uade.tpo.demo.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es-AR"));

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMine(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public NotificationResponseDTO markRead(Long notificationId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("La notificación no existe"));
        if (notification.getUser() == null || !notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Esa notificación no es tuya");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void create(User user, String title, String message) {
        if (user == null || title == null || message == null) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void bookingCancelled(Booking booking, String why) {
        ExperienceSession session = booking.getExperienceSession();
        String what = "\"" + session.getExperience().getTitle() + "\" del " + session.getStartsAt().format(DAY);
        User buyer = booking.getOrder().getUser();
        User recipient = booking.getRecipient();
        create(buyer, "Reserva cancelada", "Se canceló tu reserva para " + what + " porque " + why
                + ". Te devolvimos el dinero.");
        if (recipient != null && !recipient.getId().equals(buyer.getId())) {
            create(recipient, "Regalo cancelado", "Se canceló el regalo de " + what + " porque " + why
                    + ". El dinero vuelve a quien te lo regaló.");
        }
    }

    private NotificationResponseDTO toResponse(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
