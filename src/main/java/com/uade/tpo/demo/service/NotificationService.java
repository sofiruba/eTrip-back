package com.uade.tpo.demo.service;

import java.util.List;

import com.uade.tpo.demo.dtos.response.NotificationResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface NotificationService {
    List<NotificationResponseDTO> getMine(User user);

    NotificationResponseDTO markRead(Long notificationId, User user)
            throws ResourceNotFoundException, ForbiddenException;

    void create(User user, String title, String message);
}
