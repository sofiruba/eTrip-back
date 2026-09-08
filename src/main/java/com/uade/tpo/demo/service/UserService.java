package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.UserUpdateDTO;
import com.uade.tpo.demo.dtos.response.UserResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface UserService {
    // Mi perfil completo + contadores.
    UserResponseDTO getMyProfile(User currentUser);

    // Edita mis datos de perfil.
    UserResponseDTO updateMyProfile(User currentUser, UserUpdateDTO request) throws BadRequestException;

    // Perfil de otro usuario; datos completos solo si sos vos mismo o ADMIN.
    UserResponseDTO getUserById(Long userId, User requester) throws ResourceNotFoundException;

    // Lista todos los usuarios; solo ADMIN.
    Page<UserResponseDTO> getUsers(User requester, Pageable pageable) throws ForbiddenException;

    // Asigna un rol nuevo; solo ADMIN.
    UserResponseDTO updateRole(Long userId, String role, User requester)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;
}
