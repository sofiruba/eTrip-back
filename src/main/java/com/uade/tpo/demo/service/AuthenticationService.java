package com.uade.tpo.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.controllers.auth.AuthenticationRequest;
import com.uade.tpo.demo.controllers.auth.AuthenticationResponse;
import com.uade.tpo.demo.controllers.auth.AvailabilityResponse;
import com.uade.tpo.demo.controllers.auth.RegisterRequest;
import com.uade.tpo.demo.controllers.auth.RegisterResponse;
import com.uade.tpo.demo.controllers.auth.ResetPasswordRequest;
import com.uade.tpo.demo.controllers.auth.VerifyEmailRequest;
import com.uade.tpo.demo.controllers.config.JwtService;
import com.uade.tpo.demo.entity.CodePurpose;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.entity.VerificationCode;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.repository.VerificationCodeRepository;
import com.uade.tpo.demo.service.impl.UserServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final int CODE_MINUTES = 15;
    private static final int CODE_MAX_ATTEMPTS = 5;
    // No mandamos otro mail con código a la misma cuenta hasta que pase este tiempo
    private static final int RESEND_SECONDS = 60;
    private static final String INVALID_CODE = "El código no es válido o ya venció. Pedí uno nuevo.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository repository;
    private final VerificationCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;
    private final InterestService interestService;
    private final OrderService orderService;

    // Crea un usuario nuevo (siempre CLIENTE; los ADMIN se dan de alta a mano en la base) SIN verificar y le manda
    // por mail un código para confirmar que el email es suyo. Todavía no devuelve token: eso lo hace verifyEmail.
    // Si el mail no se puede mandar, la transacción se deshace y la cuenta no queda creada.
    @Transactional
    public RegisterResponse register(RegisterRequest request) throws BadRequestException {
        String username = trimToNull(request.getUsername());
        String email = trimToNull(request.getEmail());
        if (username == null || email == null || trimToNull(request.getPassword()) == null
                || trimToNull(request.getFirstname()) == null
                || trimToNull(request.getLastname()) == null) {
            throw new BadRequestException("Completá usuario, nombre, apellido, email y contraseña");
        }
        if (request.getPassword().trim().length() < 6) {
            throw new BadRequestException("La contraseña debe tener al menos 6 caracteres");
        }
        // El login acepta email o username y busca primero por email: si un username tuviera "@" o un email
        // no fuera un email, alguien podría registrar como email el username de otro y dejarlo sin poder entrar.
        if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 150) {
            throw new BadRequestException("Ingresá un email válido");
        }
        if (username.contains("@") || username.length() > 50) {
            throw new BadRequestException("El nombre de usuario no puede tener \"@\" ni más de 50 caracteres");
        }
        if (request.getFirstname().trim().length() > 100 || request.getLastname().trim().length() > 100) {
            throw new BadRequestException("El nombre y el apellido pueden tener hasta 100 caracteres");
        }
        if (repository.existsByDisplayUsername(username)) {
            throw new BadRequestException("Ese nombre de usuario ya está en uso");
        }
        if (repository.existsByEmail(email)) {
            throw new BadRequestException("Ya hay una cuenta con ese email");
        }

        var user = User.builder()
                .displayUsername(username)
                .firstName(request.getFirstname().trim())
                .lastName(request.getLastname().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CLIENTE)
                .interests(interestService.resolve(request.getInterests(), UserServiceImpl.MAX_INTERESTS))
                .emailVerified(false)
                .build();

        repository.save(user);
        sendCode(user, CodePurpose.VERIFY_EMAIL);
        return RegisterResponse.builder()
                .email(user.getEmail())
                .build();
    }

    // Login con email o username + password; delega la validación de la password en
    // Spring Security y devuelve un token JWT nuevo.
    // Si la contraseña es correcta pero el email no está verificado, no hay token: le mandamos un código
    // y respondemos 403 (el front pasa a la pantalla de verificar el email).
    // ForbiddenException es checked: no deshace la transacción, así el código nuevo queda guardado.
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws ForbiddenException {
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
        if (!user.isEmailVerified()) {
            sendCode(user, CodePurpose.VERIFY_EMAIL);
            throw new ForbiddenException("Todavía no verificaste tu email. Te mandamos un código para hacerlo.");
        }
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

    // Confirma el email con el código del mail y deja la sesión iniciada (devuelve el token).
    @Transactional
    public AuthenticationResponse verifyEmail(VerifyEmailRequest request) throws BadRequestException {
        User user = checkCode(request.getUsernameOrEmail(), request.getCode(), CodePurpose.VERIFY_EMAIL);
        user.setEmailVerified(true);
        repository.save(user);
        orderService.claimPendingGifts(user);
        codeRepository.deleteByUserIdAndPurpose(user.getId(), CodePurpose.VERIFY_EMAIL);
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .build();
    }

    // "Reenviar código" de la verificación. Si la cuenta no existe o ya está verificada no hace nada
    // (no le contamos a nadie qué cuentas existen ni cuáles están verificadas).
    @Transactional
    public void resendVerification(String usernameOrEmail) {
        findUser(usernameOrEmail)
                .filter(user -> !user.isEmailVerified() && user.isActive())
                .ifPresent(user -> sendCode(user, CodePurpose.VERIFY_EMAIL));
    }

    // Le manda al usuario un código para cambiar la contraseña.
    @Transactional
    public void forgotPassword(String usernameOrEmail) throws BadRequestException, ResourceNotFoundException {
        User user = findUser(usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos una cuenta con ese email o usuario"));
        if (!user.isActive()) {
            throw new BadRequestException("Esta cuenta está desactivada. Pedile a un administrador que la reactive.");
        }
        sendCode(user, CodePurpose.RESET_PASSWORD);
    }

    // Revisa el código sin cambiar la contraseña (el front lo usa para pasar al paso de la contraseña nueva).
    // Un intento fallido sí se cuenta: BadRequestException es checked, así que no deshace la transacción.
    @Transactional
    public void verifyResetCode(ResetPasswordRequest request) throws BadRequestException {
        checkCode(request.getUsernameOrEmail(), request.getCode(), CodePurpose.RESET_PASSWORD);
    }

    // Cambia la contraseña si el código es correcto, borra el código y deja al usuario logueado.
    // Como el código llegó a su mail, también queda probado que el email es suyo: la cuenta queda verificada.
    @Transactional
    public AuthenticationResponse resetPassword(ResetPasswordRequest request) throws BadRequestException {
        User user = checkCode(request.getUsernameOrEmail(), request.getCode(), CodePurpose.RESET_PASSWORD);
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("La contraseña debe tener al menos 6 caracteres");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        boolean wasVerified = user.isEmailVerified();
        user.setEmailVerified(true);
        repository.save(user);
        if (!wasVerified) {
            orderService.claimPendingGifts(user);
        }
        codeRepository.deleteByUserIdAndPurpose(user.getId(), CodePurpose.RESET_PASSWORD);
        codeRepository.deleteByUserIdAndPurpose(user.getId(), CodePurpose.VERIFY_EMAIL);
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .build();
    }

    // Genera un código de 6 dígitos, guarda su hash y lo manda por mail.
    // Pedirlo otra vez genera uno nuevo y anula el anterior (un solo código pendiente de cada tipo por usuario).
    // Si el último se mandó hace menos de un minuto no mandamos otro (sigue valiendo el anterior):
    // así nadie puede usar PLAN para llenar de mails una casilla.
    private void sendCode(User user, CodePurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        VerificationCode verificationCode = codeRepository.findByUserIdAndPurpose(user.getId(), purpose)
                .orElseGet(() -> VerificationCode.builder().user(user).purpose(purpose).build());
        if (verificationCode.getSentAt() != null && verificationCode.getExpiresAt().isAfter(now)
                && verificationCode.getSentAt().plusSeconds(RESEND_SECONDS).isAfter(now)) {
            return;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        verificationCode.setCodeHash(passwordEncoder.encode(code));
        verificationCode.setExpiresAt(now.plusMinutes(CODE_MINUTES));
        verificationCode.setSentAt(now);
        verificationCode.setAttempts(0);
        codeRepository.save(verificationCode);
        if (purpose == CodePurpose.VERIFY_EMAIL) {
            mailService.sendVerificationCode(user, code, CODE_MINUTES);
        } else {
            mailService.sendPasswordResetCode(user, code, CODE_MINUTES);
        }
    }

    // Devuelve el usuario si el código es válido. Cada intento fallido suma uno; al llegar
    // al máximo (o si ya venció) el código se borra y hay que pedir otro.
    private User checkCode(String usernameOrEmail, String rawCode, CodePurpose purpose) throws BadRequestException {
        User user = findUser(usernameOrEmail).orElseThrow(() -> new BadRequestException(INVALID_CODE));
        VerificationCode verificationCode = codeRepository.findByUserIdAndPurpose(user.getId(), purpose)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE));
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            codeRepository.delete(verificationCode);
            throw new BadRequestException(INVALID_CODE);
        }
        String code = trimToNull(rawCode);
        if (code == null || !passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            int attempts = verificationCode.getAttempts() + 1;
            if (attempts >= CODE_MAX_ATTEMPTS) {
                codeRepository.delete(verificationCode);
                throw new BadRequestException("Hubo demasiados intentos. Pedí un código nuevo.");
            }
            verificationCode.setAttempts(attempts);
            codeRepository.save(verificationCode);
            throw new BadRequestException("El código no es correcto");
        }
        return user;
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
