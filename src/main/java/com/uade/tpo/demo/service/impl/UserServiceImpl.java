package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

import com.uade.tpo.demo.dtos.request.UserUpdateDTO;
import com.uade.tpo.demo.dtos.response.UserResponseDTO;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.Interest;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.repository.ReviewRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.InterestService;
import com.uade.tpo.demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** Hasta cuántos intereses puede elegir una persona (el registro usa el mismo límite). */
    public static final int MAX_INTERESTS = 20;

    private final UserRepository userRepository;
    private final ExperienceRepository experienceRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final InterestService interestService;

    // Mi perfil completo + contadores. El usuario sale del token, no de la URL.
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getMyProfile(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElse(currentUser);
        return toResponse(user, true);
    }

    // Edita mi perfil: nombre y apellido, y lo público (presentación, ciudad, foto, intereses).
    // Un campo que no viene se deja como está; un texto vacío lo borra.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UserResponseDTO updateMyProfile(User currentUser, UserUpdateDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException("Faltan los datos del perfil");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado inexistente"));
        String firstName = trimToNull(request.getFirstName());
        String lastName = trimToNull(request.getLastName());
        if ((firstName != null && firstName.length() > 100) || (lastName != null && lastName.length() > 100)) {
            throw new BadRequestException("El nombre y el apellido pueden tener hasta 100 caracteres");
        }
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (request.getBio() != null) {
            String bio = trimToNull(request.getBio());
            if (bio != null && bio.length() > 500) {
                throw new BadRequestException("La presentación puede tener hasta 500 caracteres");
            }
            user.setBio(bio);
        }
        if (request.getCity() != null) {
            String city = trimToNull(request.getCity());
            if (city != null && city.length() > 100) {
                throw new BadRequestException("La ciudad puede tener hasta 100 caracteres");
            }
            user.setCity(city);
        }
        if (request.getAvatarBase64() != null) {
            user.setAvatar(decodeAvatar(request.getAvatarBase64()));
        }
        if (request.getInterests() != null) {
            user.setInterests(interestService.resolve(request.getInterests(), MAX_INTERESTS));
        }
        return toResponse(userRepository.save(user), true);
    }

    // Foto de perfil: viene como data URL ("data:image/jpeg;base64,...") o base64 pelado; vacío = sin foto.
    // El front la achica a 320 px antes de mandarla; igual se pone un tope.
    private byte[] decodeAvatar(String value) throws BadRequestException {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int comma = trimmed.indexOf(',');
        String base64 = trimmed.startsWith("data:") && comma != -1 ? trimmed.substring(comma + 1) : trimmed;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("La foto de perfil no es válida");
        }
        if (bytes.length > 400 * 1024) {
            throw new BadRequestException("La foto de perfil es demasiado grande");
        }
        return bytes;
    }

    // Perfil de otro usuario; datos completos solo si sos vos mismo o ADMIN.
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId, User requester) throws ResourceNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));
        // requester es null cuando lo pide un invitado (la ruta es pública): ve el perfil reducido
        boolean full = isAdmin(requester) || (requester != null && requester.getId().equals(userId));
        return toResponse(user, full);
    }

    // Lista todos los usuarios; solo ADMIN.
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getUsers(User requester, Pageable pageable) throws ForbiddenException {
        if (!isAdmin(requester)) {
            throw new ForbiddenException("Solo un administrador puede ver los usuarios");
        }
        return userRepository.findAll(pageable).map(user -> toResponse(user, true));
    }

    // Asigna un rol nuevo; solo ADMIN, y no se puede auto-asignar (evita quedarse sin acceso).
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UserResponseDTO updateRole(Long userId, String role, User requester)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        if (!isAdmin(requester)) {
            throw new ForbiddenException("Solo un administrador puede cambiar roles");
        }
        if (requester.getId().equals(userId)) {
            throw new ForbiddenException("No podés cambiar tu propio rol");
        }

        Role newRole = parseRole(role);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));
        user.setRole(newRole);

        return toResponse(userRepository.save(user), true);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UserResponseDTO updateStatus(Long userId, boolean active, User requester)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        if (!isAdmin(requester)) {
            throw new ForbiddenException("Solo un administrador puede activar o desactivar usuarios");
        }
        if (requester.getId().equals(userId)) {
            throw new ForbiddenException("No podés desactivar tu propio usuario");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario solicitado no existe"));
        user.setActive(active);
        return toResponse(userRepository.save(user), true);
    }

    private Role parseRole(String role) throws BadRequestException {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("El rol tiene que ser CLIENTE o ADMIN");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("El rol tiene que ser CLIENTE o ADMIN");
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

    // Arma el DTO; includePrivate agrega rol, estado, email y contadores (solo para el dueño o ADMIN).
    private UserResponseDTO toResponse(User user, boolean includePrivate) {
        UserResponseDTO.UserResponseDTOBuilder builder = UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getDisplayUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .city(user.getCity())
                .avatarBase64(user.getAvatar() != null ? Base64.getEncoder().encodeToString(user.getAvatar()) : null)
                .interests(interestNames(user.getInterests()))
                .publishedExperiences(experienceRepository.countByPublisherIdAndDeletedAtIsNull(user.getId()));

        if (includePrivate) {
            builder.role(user.getRole() != null ? user.getRole().name() : null)
                    .active(user.isActive())
                    .email(user.getEmail())
                    .bookingsCount(orderRepository.countByUserId(user.getId()))
                    .reviewsCount(reviewRepository.countByUserId(user.getId()));
        }

        return builder.build();
    }

    /** Nombres de los intereses, como los usa el front. */
    public static List<String> interestNames(java.util.Collection<Interest> interests) {
        return interests == null ? List.of() : interests.stream().map(Interest::getName).toList();
    }
}
