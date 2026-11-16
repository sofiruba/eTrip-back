package com.uade.tpo.demo.controllers.favorites;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.response.FavoriteResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.FavoriteService;

import lombok.RequiredArgsConstructor;

/** Experiencias guardadas ("Guardados") del usuario que inició sesión. */
@RestController
@RequestMapping("favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final FavoriteService favoriteService;

    // Mis guardados (ids de experiencia, la más reciente primero).
    @GetMapping
    public ResponseEntity<List<FavoriteResponseDTO>> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(favoriteService.getMine(user));
    }

    // Guardar una experiencia.
    @PostMapping("/{experienceId}")
    public ResponseEntity<FavoriteResponseDTO> add(@PathVariable Long experienceId, @AuthenticationPrincipal User user)
            throws ResourceNotFoundException, BadRequestException {
        return ResponseEntity.ok(favoriteService.add(experienceId, user));
    }

    // Quitarla de los guardados.
    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Void> remove(@PathVariable Long experienceId, @AuthenticationPrincipal User user) {
        favoriteService.remove(experienceId, user);
        return ResponseEntity.noContent().build();
    }
}
