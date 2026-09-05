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
    UserResponseDTO getMyProfile(User currentUser);

    UserResponseDTO updateMyProfile(User currentUser, UserUpdateDTO request) throws BadRequestException;

    UserResponseDTO getUserById(Long userId, User requester) throws ResourceNotFoundException;

    Page<UserResponseDTO> getUsers(User requester, Pageable pageable) throws ForbiddenException;

    UserResponseDTO updateRole(Long userId, String role, User requester)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;
}
