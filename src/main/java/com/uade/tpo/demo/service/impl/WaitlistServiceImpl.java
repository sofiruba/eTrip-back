package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.entity.WaitlistEntry;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.WaitlistRepository;
import com.uade.tpo.demo.service.NotificationService;
import com.uade.tpo.demo.service.WaitlistService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WaitlistServiceImpl implements WaitlistService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final WaitlistRepository waitlistRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMySessionIds(User user) {
        return waitlistRepository.findByUserId(user.getId()).stream()
                .map(entry -> entry.getSession().getId())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void join(Long sessionId, User user) throws ResourceNotFoundException, BadRequestException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("La fecha no existe"));
        Experience experience = session.getExperience();
        if (!session.isActive() || experience == null || !experience.isActive()
                || session.getStartsAt() == null || session.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Esta fecha ya no está disponible");
        }
        if (session.getAvailableSeats() != null && session.getAvailableSeats() > 0) {
            throw new BadRequestException("Esta fecha todavía tiene lugares: podés reservar directamente");
        }
        if (experience.getPublisher() != null && experience.getPublisher().getId().equals(user.getId())) {
            throw new BadRequestException("Es tu propia experiencia");
        }
        if (waitlistRepository.existsByUserIdAndSessionId(user.getId(), sessionId)) {
            return;
        }
        waitlistRepository.save(WaitlistEntry.builder()
                .user(user)
                .session(session)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void leave(Long sessionId, User user) {
        waitlistRepository.deleteByUserIdAndSessionId(user.getId(), sessionId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void notifySeatsAvailable(ExperienceSession session) {
        if (session == null || !session.isActive() || session.getStartsAt() == null
                || session.getStartsAt().isBefore(LocalDateTime.now())
                || session.getAvailableSeats() == null || session.getAvailableSeats() <= 0) {
            return;
        }
        List<WaitlistEntry> entries = waitlistRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        if (entries.isEmpty()) {
            return;
        }
        String title = session.getExperience() != null ? session.getExperience().getTitle() : "la experiencia";
        for (WaitlistEntry entry : entries) {
            notificationService.create(entry.getUser(), "¡Se liberó un lugar!",
                    "Hay lugar en \"" + title + "\" el " + session.getStartsAt().format(DATE)
                            + ". Reservá antes de que se vuelva a agotar.");
        }
        waitlistRepository.deleteAll(entries);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void notifySessionCancelled(ExperienceSession session) {
        List<WaitlistEntry> entries = waitlistRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        String title = session.getExperience() != null ? session.getExperience().getTitle() : "la experiencia";
        for (WaitlistEntry entry : entries) {
            notificationService.create(entry.getUser(), "Fecha cancelada",
                    "La fecha de \"" + title + "\" del " + session.getStartsAt().format(DATE)
                            + " en la que estabas en lista de espera se canceló.");
        }
        waitlistRepository.deleteAll(entries);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Long sessionId) {
        return waitlistRepository.countBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void clear(Long sessionId) {
        waitlistRepository.deleteBySessionId(sessionId);
    }
}
