package com.uade.tpo.demo.controllers.experiences;

import java.io.IOException;
import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experiences")
@RequiredArgsConstructor
public class ExperiencesController {

    private final ExperienceService experienceService;

    @GetMapping
    public ResponseEntity<Page<ExperienceResponseDTO>> getExperiences(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page == null || size == null)
            return ResponseEntity.ok(experienceService.getExperiences(PageRequest.of(0, Integer.MAX_VALUE)));
        return ResponseEntity.ok(experienceService.getExperiences(PageRequest.of(page, size)));
    }

    @GetMapping("/{experienceId}")
    public ResponseEntity<ExperienceResponseDTO> getExperienceById(@PathVariable Long experienceId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceService.getExperienceById(experienceId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperienceResponseDTO> createExperience(
            @RequestPart("experience") ExperienceRequestDTO request,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal User publisher) throws ResourceNotFoundException, BadRequestException, IOException {
        ExperienceResponseDTO result = experienceService.createExperience(request, image, publisher.getId());
        return ResponseEntity.created(URI.create("/experiences/" + result.getId())).body(result);
    }

    @PutMapping(value = "/{experienceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperienceResponseDTO> updateExperience(
            @PathVariable Long experienceId,
            @RequestPart("experience") ExperienceRequestDTO request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException, IOException {
        return ResponseEntity.ok(experienceService.updateExperience(experienceId, request, image, currentUser));
    }

    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Void> deleteExperience(
            @PathVariable Long experienceId,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        experienceService.deleteExperience(experienceId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
