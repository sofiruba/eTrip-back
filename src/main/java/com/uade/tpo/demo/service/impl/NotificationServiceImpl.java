package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.response.NotificationResponseDTO;
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
                .orElseThrow(ResourceNotFoundException::new);
        if (notification.getUser() == null || !notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException();
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
