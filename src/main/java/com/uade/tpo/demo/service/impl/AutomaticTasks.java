package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tareas que el back hace solo, sin que nadie llame a un endpoint (@EnableScheduling en DemoApplication).
 * Corre 10 segundos después de arrancar y después cada 10 minutos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutomaticTasks {

    private static final long EVERY_10_MINUTES = 10 * 60 * 1000;

    private final ExperienceSessionRepository experienceSessionRepository;
    private final CartItemRepository cartItemRepository;
    private final ExperienceRepository experienceRepository;

    @Scheduled(initialDelay = 10_000, fixedRate = EVERY_10_MINUTES)
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Las fechas cuya hora de fin ya pasó quedan "Finalizadas"
        int finished = experienceSessionRepository.markFinished(now);

        // 2. Se sacan de los carritos las fechas canceladas, terminadas o que ya empezaron
        int removedFromCarts = cartItemRepository.deleteUnavailable(now);

        // 3. Las experiencias sin ninguna fecha futura se pausan (dejan de verse en el catálogo).
        //    Se vuelven a publicar solas cuando el anfitrión les agrega una fecha (ExperienceSessionServiceImpl).
        int paused = experienceRepository.pauseWithoutUpcomingSessions(now);

        if (finished + removedFromCarts + paused > 0) {
            log.info("Tareas automáticas: {} fechas finalizadas, {} ítems quitados de carritos, {} experiencias pausadas",
                    finished, removedFromCarts, paused);
        }
    }
}
