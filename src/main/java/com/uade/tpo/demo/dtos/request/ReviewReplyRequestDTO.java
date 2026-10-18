package com.uade.tpo.demo.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta del anfitrión a una reseña. Vacía o null la borra. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyRequestDTO {
    private String reply;
}
