package com.uade.tpo.demo.controllers.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.AuthenticationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    // Crea un usuario nuevo (rol CLIENTE por defecto) y devuelve el token de una vez.
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) throws BadRequestException {
        return ResponseEntity.ok(service.register(request));
    }

    // Login con email o username + password, devuelve el token JWT.
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    // ¿Están libres este username y/o este email? (el registro lo consulta antes de seguir)
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> availability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(service.checkAvailability(username, email));
    }

    // Manda un código para recuperar la contraseña (404 si la cuenta no existe, 400 si está desactivada).
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request)
            throws BadRequestException, ResourceNotFoundException {
        return ResponseEntity.ok(service.forgotPassword(request.getUsernameOrEmail()));
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
