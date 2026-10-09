package com.uade.tpo.demo.controllers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.uade.tpo.demo.entity.Role;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(req -> req.requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/error/**").permitAll()
                                .requestMatchers("/users/me").hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/experiences/**", "/experience-categories/**",
                                                "/experience-sessions/**", "/reviews/experience/**", "/users/*").permitAll()
                                                .requestMatchers("/users/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/experience-categories/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/experiences/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/experience-sessions/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/carts/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/orders/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/bookings/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/notifications/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/reviews/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers(HttpMethod.GET, "/discount-coupons/**")
                                                .hasAnyAuthority(Role.CLIENTE.name(), Role.ADMIN.name())
                                                .requestMatchers("/discount-coupons/**")
                                                .hasAnyAuthority(Role.ADMIN.name())
                                                .anyRequest()
                                                .authenticated())
                                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(java.util.List.of("http://localhost:5173", "http://localhost:3000"));
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
