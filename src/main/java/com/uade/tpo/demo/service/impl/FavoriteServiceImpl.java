package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.response.FavoriteResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.Favorite;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.FavoriteRepository;
import com.uade.tpo.demo.service.FavoriteService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ExperienceRepository experienceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResponseDTO> getMine(User user) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public FavoriteResponseDTO add(Long experienceId, User user) throws ResourceNotFoundException, BadRequestException {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("La experiencia no existe"));
        if (experience.getDeletedAt() != null) {
            throw new BadRequestException("Esta experiencia ya no está disponible");
        }
        if (experience.getPublisher() != null && experience.getPublisher().getId().equals(user.getId())) {
            throw new BadRequestException("Es tu propia experiencia");
        }
        if (favoriteRepository.existsByUserIdAndExperienceId(user.getId(), experienceId)) {
            return FavoriteResponseDTO.builder().experienceId(experienceId).createdAt(LocalDateTime.now()).build();
        }
        Favorite saved = favoriteRepository.save(Favorite.builder()
                .user(user)
                .experience(experience)
                .createdAt(LocalDateTime.now())
                .build());
        return toResponse(saved);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void remove(Long experienceId, User user) {
        favoriteRepository.deleteByUserIdAndExperienceId(user.getId(), experienceId);
    }

    private FavoriteResponseDTO toResponse(Favorite favorite) {
        return FavoriteResponseDTO.builder()
                .experienceId(favorite.getExperience().getId())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
