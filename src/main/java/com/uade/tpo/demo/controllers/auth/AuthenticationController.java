package com.uade.tpo.demo.controllers.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.AuthenticationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    // Crea un usuario nuevo (rol CLIENTE) sin verificar y le manda un código al email. Devuelve { email }, todavía sin token.
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request) throws BadRequestException {
        return ResponseEntity.ok(service.register(request));
    }

    // Confirma el email con el código y devuelve el token (queda logueado).
    @PostMapping("/verify-email")
    public ResponseEntity<AuthenticationResponse> verifyEmail(@RequestBody VerifyEmailRequest request)
            throws BadRequestException {
        return ResponseEntity.ok(service.verifyEmail(request));
    }

    // Manda otro código de verificación (204 siempre, exista o no la cuenta).
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ForgotPasswordRequest request) {
        service.resendVerification(request.getUsernameOrEmail());
        return ResponseEntity.noContent().build();
    }

    // Login con email o username + password, devuelve el token JWT.
    // 403 si el email todavía no está verificado (y le manda un código nuevo).
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) throws ForbiddenException {
        return ResponseEntity.ok(service.authenticate(request));
    }

    // ¿Están libres este username y/o este email? (el registro lo consulta antes de seguir)
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> availability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(service.checkAvailability(username, email));
    }

    // Manda por mail un código para recuperar la contraseña (204; 404 si la cuenta no existe, 400 si está desactivada).
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request)
            throws BadRequestException, ResourceNotFoundException {
        service.forgotPassword(request.getUsernameOrEmail());
        return ResponseEntity.noContent().build();
    }

    // Revisa el código sin cambiar la contraseña (204 si es válido, 400 si no).
    @PostMapping("/verify-reset-code")
    public ResponseEntity<Void> verifyResetCode(@RequestBody ResetPasswordRequest request)
            throws BadRequestException {
        service.verifyResetCode(request);
        return ResponseEntity.noContent().build();
    }

    // Cambia la contraseña con el código y devuelve el token (queda logueado).
    @PostMapping("/reset-password")
    public ResponseEntity<AuthenticationResponse> resetPassword(@RequestBody ResetPasswordRequest request)
            throws BadRequestException {
        return ResponseEntity.ok(service.resetPassword(request));
    }
}
