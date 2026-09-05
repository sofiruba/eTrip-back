package com.uade.tpo.demo.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.uade.tpo.demo.controllers.auth.AuthenticationRequest;
import com.uade.tpo.demo.controllers.auth.AuthenticationResponse;
import com.uade.tpo.demo.controllers.auth.RegisterRequest;
import com.uade.tpo.demo.controllers.config.JwtService;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
        private final UserRepository repository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthenticationResponse register(RegisterRequest request) throws BadRequestException {
                String username = trimToNull(request.getUsername());
                String email = trimToNull(request.getEmail());
                if (username == null || email == null || trimToNull(request.getPassword()) == null
                                || trimToNull(request.getFirstname()) == null
                                || trimToNull(request.getLastname()) == null) {
                        throw new BadRequestException();
                }
                if (repository.existsByDisplayUsername(username)) {
                        throw new BadRequestException();
                }
                if (repository.existsByEmail(email)) {
                        throw new BadRequestException();
                }

                // El registro publico siempre crea CLIENTE. Los ADMIN se dan de alta a mano
                // en la base (UPDATE user SET role='ADMIN' WHERE email=...), no por la API.
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

        private String trimToNull(String value) {
                if (value == null) {
                        return null;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }
}
