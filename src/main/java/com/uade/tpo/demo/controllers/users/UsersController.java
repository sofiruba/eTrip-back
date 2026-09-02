package com.uade.tpo.demo.controllers.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.UserUpdateDTO;
import com.uade.tpo.demo.dtos.response.UserResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getMyProfile(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @RequestBody UserUpdateDTO request,
            @AuthenticationPrincipal User currentUser) throws BadRequestException {
        return ResponseEntity.ok(userService.updateMyProfile(currentUser, request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException {
        return ResponseEntity.ok(userService.getUserById(userId, currentUser));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) throws ForbiddenException {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(userService.getUsers(currentUser, pageRequest));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponseDTO> updateRole(
            @PathVariable Long userId,
            @RequestParam String role,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        return ResponseEntity.ok(userService.updateRole(userId, role, currentUser));
    }
}
