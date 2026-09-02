package com.uade.tpo.demo.controllers.experiences;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
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
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo)
            throws ResourceNotFoundException, BadRequestException {
        ExperienceSearchDTO filter = ExperienceSearchDTO.builder()
                .categoryId(categoryId)
                .title(title)
                .location(location)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .publisherId(publisherId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        return ResponseEntity.ok(experienceService.searchExperiences(filter, pageRequest(page, size)));
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ExperienceResponseDTO>> getMyExperiences(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException, BadRequestException {
        ExperienceSearchDTO filter = ExperienceSearchDTO.builder()
                .publisherId(currentUser.getId())
                .build();

        return ResponseEntity.ok(experienceService.searchExperiences(filter, pageRequest(page, size)));
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

    private PageRequest pageRequest(Integer page, Integer size) {
        return page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);
    }
}
