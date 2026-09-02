package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.UserUpdateDTO;
import com.uade.tpo.demo.dtos.response.UserResponseDTO;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.repository.ReviewRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ExperienceRepository experienceRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getMyProfile(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElse(currentUser);
        return toResponse(user, true);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UserResponseDTO updateMyProfile(User currentUser, UserUpdateDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException();
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado inexistente"));

        String firstName = trimToNull(request.getFirstName());
        String lastName = trimToNull(request.getLastName());
        if (firstName == null && lastName == null) {
            throw new BadRequestException();
        }
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

        return toResponse(userRepository.save(user), true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId, User requester) throws ResourceNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);
        boolean full = isAdmin(requester) || requester.getId().equals(userId);
        return toResponse(user, full);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getUsers(User requester, Pageable pageable) throws ForbiddenException {
        if (!isAdmin(requester)) {
            throw new ForbiddenException();
        }
        return userRepository.findAll(pageable).map(user -> toResponse(user, true));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UserResponseDTO updateRole(Long userId, String role, User requester)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        if (!isAdmin(requester)) {
            throw new ForbiddenException();
        }
        if (requester.getId().equals(userId)) {
            // Un ADMIN no puede cambiarse el rol a si mismo (evita quedarse sin acceso).
            throw new ForbiddenException();
        }

        Role newRole = parseRole(role);
        User user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);
        user.setRole(newRole);

        return toResponse(userRepository.save(user), true);
    }

    private Role parseRole(String role) throws BadRequestException {
        if (role == null || role.isBlank()) {
            throw new BadRequestException();
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException();
        }
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserResponseDTO toResponse(User user, boolean includePrivate) {
        UserResponseDTO.UserResponseDTOBuilder builder = UserResponseDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .publishedExperiences(experienceRepository.countByPublisherId(user.getId()));

        if (includePrivate) {
            builder.email(user.getEmail())
                    .bookingsCount(orderRepository.countByUserId(user.getId()))
                    .reviewsCount(reviewRepository.countByUserId(user.getId()));
        }

        return builder.build();
    }
}
