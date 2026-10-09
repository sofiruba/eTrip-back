package com.uade.tpo.demo.controllers.notifications;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.uade.tpo.demo.dtos.response.NotificationResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
public class NotificationsController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getMine(user));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user)
            throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(notificationService.markRead(notificationId, user));
    }
}
