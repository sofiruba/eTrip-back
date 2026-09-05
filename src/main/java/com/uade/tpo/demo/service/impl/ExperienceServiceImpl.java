package com.uade.tpo.demo.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceCategory;
import com.uade.tpo.demo.entity.ExperienceImage;
import com.uade.tpo.demo.entity.Review;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSpecifications;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final ExperienceCategoryRepository experienceCategoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceResponseDTO> getExperiences(Pageable pageable) {
        return experienceRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceResponseDTO> searchExperiences(ExperienceSearchDTO filter, Pageable pageable)
            throws ResourceNotFoundException, BadRequestException {
        if (filter == null) {
            return getExperiences(pageable);
        }

        if (filter.getCategoryId() != null) {
            validateCategoryExists(filter.getCategoryId());
        }
        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            throw new BadRequestException();
        }
        if (filter.getDateFrom() != null && filter.getDateTo() != null
                && filter.getDateFrom().isAfter(filter.getDateTo())) {
            throw new BadRequestException();
        }

        return experienceRepository.findAll(ExperienceSpecifications.withFilters(filter), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienceResponseDTO getExperienceById(Long experienceId) throws ResourceNotFoundException {
        return toResponse(findExperience(experienceId));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO createExperience(ExperienceRequestDTO request, List<MultipartFile> images,
            Long publisherId)
            throws ResourceNotFoundException, BadRequestException, IOException {
        validateData(request);
        List<MultipartFile> validImages = nonEmptyImages(images);
        if (publisherId == null || validImages.isEmpty()) {
            throw new BadRequestException();
        }

        User publisher = userRepository.findById(publisherId)
                .orElseThrow(ResourceNotFoundException::new);
        ExperienceCategory category = experienceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(ResourceNotFoundException::new);

        Experience experience = Experience.builder()
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .price(request.getPrice())
                .location(trimToNull(request.getLocation()))
                .category(category)
                .publisher(publisher)
                .build();

        experience.setImages(toImageEntities(validImages, experience));

        return toResponse(experienceRepository.save(experience));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO updateExperience(Long experienceId, ExperienceRequestDTO request,
            List<MultipartFile> images, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException, IOException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);
        validateData(request);

        if (!request.getCategoryId().equals(experience.getCategory().getId())) {
            ExperienceCategory category = experienceCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(ResourceNotFoundException::new);
            experience.setCategory(category);
        }

        experience.setTitle(request.getTitle().trim());
        experience.setDescription(trimToNull(request.getDescription()));
        experience.setPrice(request.getPrice());
        experience.setLocation(trimToNull(request.getLocation()));

        List<MultipartFile> validImages = nonEmptyImages(images);
        if (!validImages.isEmpty()) {
            // Reemplaza el set completo de fotos (orphanRemoval borra las viejas).
            if (experience.getImages() != null) {
                experience.getImages().clear();
            } else {
                experience.setImages(new ArrayList<>());
            }
            experience.getImages().addAll(toImageEntities(validImages, experience));
        }

        return toResponse(experienceRepository.save(experience));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO updateDiscount(Long experienceId, BigDecimal discountPercentage, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);

        if (discountPercentage == null || discountPercentage.signum() == 0) {
            experience.setDiscountPercentage(null);
        } else {
            if (discountPercentage.signum() < 0 || discountPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                throw new BadRequestException();
            }
            experience.setDiscountPercentage(discountPercentage);
        }

        return toResponse(experienceRepository.save(experience));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteExperience(Long experienceId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);

        if (experience.getSessions() != null && !experience.getSessions().isEmpty()) {
            throw new BadRequestException();
        }

        experienceRepository.delete(experience);
    }

    private Experience findExperience(Long experienceId) throws ResourceNotFoundException {
        return experienceRepository.findById(experienceId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private void validateCategoryExists(Long categoryId) throws ResourceNotFoundException {
        if (!experienceCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException();
        }
    }

    private void assertCanManage(Experience experience, User currentUser) throws ForbiddenException {
        boolean isOwner = experience.getPublisher() != null
                && currentUser != null
                && experience.getPublisher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException();
        }
    }

    private void validateData(ExperienceRequestDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException();
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException();
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BadRequestException();
        }
        if (request.getCategoryId() == null) {
            throw new BadRequestException();
        }
    }

    private List<MultipartFile> nonEmptyImages(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        List<MultipartFile> result = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                result.add(image);
            }
        }
        return result;
    }

    private List<ExperienceImage> toImageEntities(List<MultipartFile> images, Experience experience)
            throws IOException {
        List<ExperienceImage> result = new ArrayList<>();
        int position = 0;
        for (MultipartFile image : images) {
            result.add(ExperienceImage.builder()
                    .image(image.getBytes())
                    .position(position++)
                    .experience(experience)
                    .build());
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExperienceResponseDTO toResponse(Experience experience) {
        List<Review> reviews = experience.getReviews();
        int reviewCount = reviews != null ? reviews.size() : 0;
        Double averageRating = reviewCount > 0
                ? reviews.stream().mapToInt(Review::getRating).average().orElse(0)
                : null;

        List<String> imagesBase64 = new ArrayList<>();
        if (experience.getImages() != null) {
            for (ExperienceImage image : experience.getImages()) {
                if (image.getImage() != null) {
                    imagesBase64.add(Base64.getEncoder().encodeToString(image.getImage()));
                }
            }
        }

        return ExperienceResponseDTO.builder()
                .id(experience.getId())
                .title(experience.getTitle())
                .description(experience.getDescription())
                .price(experience.getPrice())
                .discountPercentage(experience.getDiscountPercentage())
                .finalPrice(experience.getEffectivePrice())
                .location(experience.getLocation())
                .imagesBase64(imagesBase64)
                .categoryId(experience.getCategory() != null ? experience.getCategory().getId() : null)
                .categoryName(experience.getCategory() != null ? experience.getCategory().getName() : null)
                .publisherId(experience.getPublisher() != null ? experience.getPublisher().getId() : null)
                .publisherName(publisherName(experience.getPublisher()))
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
    }

    private String publisherName(User publisher) {
        if (publisher == null) {
            return null;
        }
        String first = publisher.getFirstName() != null ? publisher.getFirstName() : "";
        String last = publisher.getLastName() != null ? publisher.getLastName() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? publisher.getEmail() : fullName;
    }
}
