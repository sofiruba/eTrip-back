package com.uade.tpo.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.uade.tpo.demo.controllers.auth.AuthenticationRequest;
import com.uade.tpo.demo.controllers.auth.AuthenticationResponse;
import com.uade.tpo.demo.controllers.auth.AvailabilityResponse;
import com.uade.tpo.demo.controllers.auth.ForgotPasswordResponse;
import com.uade.tpo.demo.controllers.auth.ResetPasswordRequest;
import com.uade.tpo.demo.controllers.auth.RegisterRequest;
import com.uade.tpo.demo.controllers.config.JwtService;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final int RESET_CODE_MINUTES = 15;
    private static final int RESET_CODE_MAX_ATTEMPTS = 5;
    private static final String INVALID_CODE = "El código no es válido o ya venció. Pedí uno nuevo.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetSender passwordResetSender;

    // true solo en desarrollo: la respuesta de /forgot-password incluye el código (no hay mail todavía)
    @Value("${app.password-reset.show-code:false}")
    private boolean showResetCode;

    // Crea un usuario nuevo (siempre CLIENTE; los ADMIN se dan de alta a mano en la base)
    // y devuelve el token de una vez, sin exigir un login aparte.
    public AuthenticationResponse register(RegisterRequest request) throws BadRequestException {
        String username = trimToNull(request.getUsername());
        String email = trimToNull(request.getEmail());
        if (username == null || email == null || trimToNull(request.getPassword()) == null
                || trimToNull(request.getFirstname()) == null
                || trimToNull(request.getLastname()) == null) {
            throw new BadRequestException();
        }
        if (request.getPassword().trim().length() < 6) {
            throw new BadRequestException("La contraseña debe tener al menos 6 caracteres");
        }
        if (repository.existsByDisplayUsername(username)) {
            throw new BadRequestException("Ese nombre de usuario ya está en uso");
        }
        if (repository.existsByEmail(email)) {
            throw new BadRequestException("Ya hay una cuenta con ese email");
        }

        var user = User.builder()
                .displayUsername(username)
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CLIENTE)
                .build();

        repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }

    // Login con email o username + password; delega la validación de la password en
    // Spring Security y devuelve un token JWT nuevo.
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String identifier = request.getUsernameOrEmail();
        String email = repository.findByEmail(identifier)
                .map(User::getEmail)
                .or(() -> repository.findByDisplayUsername(identifier).map(User::getEmail))
                .orElse(identifier);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()));

        var user = repository.findByEmail(email)
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }

    // Para que el registro avise antes de seguir si el username o el email ya existen.
    // Cada parámetro es opcional: si no viene, esa parte de la respuesta queda en null.
    public AvailabilityResponse checkAvailability(String username, String email) {
        String cleanUsername = trimToNull(username);
        String cleanEmail = trimToNull(email);
        return AvailabilityResponse.builder()
                .usernameAvailable(cleanUsername == null ? null : !repository.existsByDisplayUsername(cleanUsername))
                .emailAvailable(cleanEmail == null ? null : !repository.existsByEmail(cleanEmail))
                .build();
    }

    // Genera un código de 6 dígitos y se lo manda al usuario (ver PasswordResetSender).
    // Pedirlo otra vez genera uno nuevo y anula el anterior.
    public ForgotPasswordResponse forgotPassword(String usernameOrEmail)
            throws BadRequestException, ResourceNotFoundException {
        User user = findUser(usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos una cuenta con ese email o usuario"));
        if (!user.isActive()) {
            throw new BadRequestException("Esta cuenta está desactivada. Pedile a un administrador que la reactive.");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setResetCodeHash(passwordEncoder.encode(code));
        user.setResetCodeExpiresAt(LocalDateTime.now().plusMinutes(RESET_CODE_MINUTES));
        user.setResetCodeAttempts(0);
        repository.save(user);
        passwordResetSender.send(user, code);
        return ForgotPasswordResponse.builder()
                .devCode(showResetCode ? code : null)
                .build();
    }

    // Revisa el código sin cambiar nada (el front lo usa para pasar al paso de la contraseña nueva).
    public void verifyResetCode(ResetPasswordRequest request) throws BadRequestException {
        checkResetCode(request);
    }

    // Cambia la contraseña si el código es correcto y deja al usuario logueado.
    public AuthenticationResponse resetPassword(ResetPasswordRequest request) throws BadRequestException {
        User user = checkResetCode(request);
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("La contraseña debe tener al menos 6 caracteres");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        clearResetCode(user);
        repository.save(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .build();
    }

    // Devuelve el usuario si el código es válido. Cada intento fallido suma uno; al llegar
    // al máximo el código se invalida y hay que pedir otro.
    private User checkResetCode(ResetPasswordRequest request) throws BadRequestException {
        User user = findUser(request.getUsernameOrEmail()).orElseThrow(() -> new BadRequestException(INVALID_CODE));
        String code = trimToNull(request.getCode());
        boolean usable = user.getResetCodeHash() != null
                && user.getResetCodeExpiresAt() != null
                && user.getResetCodeExpiresAt().isAfter(LocalDateTime.now());
        if (!usable || code == null) {
            throw new BadRequestException(INVALID_CODE);
        }
        if (!passwordEncoder.matches(code, user.getResetCodeHash())) {
            int attempts = (user.getResetCodeAttempts() == null ? 0 : user.getResetCodeAttempts()) + 1;
            if (attempts >= RESET_CODE_MAX_ATTEMPTS) {
                clearResetCode(user);
                repository.save(user);
                throw new BadRequestException("Hubo demasiados intentos. Pedí un código nuevo.");
            }
            user.setResetCodeAttempts(attempts);
            repository.save(user);
            throw new BadRequestException("El código no es correcto");
        }
        return user;
    }

    private void clearResetCode(User user) {
        user.setResetCodeHash(null);
        user.setResetCodeExpiresAt(null);
        user.setResetCodeAttempts(null);
    }

    private Optional<User> findUser(String usernameOrEmail) {
        String identifier = trimToNull(usernameOrEmail);
        if (identifier == null) {
            return Optional.empty();
        }
        return repository.findByEmail(identifier).or(() -> repository.findByDisplayUsername(identifier));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
