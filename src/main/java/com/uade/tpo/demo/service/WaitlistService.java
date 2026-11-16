package com.uade.tpo.demo.service;

import java.util.List;

import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

/** Lista de espera de las fechas agotadas: "avisame si se libera un lugar". */
public interface WaitlistService {

    // Ids de las fechas en las que el usuario está anotado.
    List<Long> getMySessionIds(User user);

    // Se anota en una fecha agotada (activa y futura, de otra persona).
    void join(Long sessionId, User user) throws ResourceNotFoundException, BadRequestException;

    // Se baja de la lista.
    void leave(Long sessionId, User user);

    // Se liberaron lugares en la fecha: avisa a todos los anotados y los saca de la lista (se avisa una sola vez).
    void notifySeatsAvailable(ExperienceSession session);

    // La fecha se canceló: avisa a los anotados y vacía la lista.
    void notifySessionCancelled(ExperienceSession session);

    long count(Long sessionId);

    // Borra la lista de una fecha (cuando la fecha se elimina del todo).
    void clear(Long sessionId);
}
