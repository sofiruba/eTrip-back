package com.uade.tpo.demo.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** La compra es un regalo: para quién es (obligatorio), su email (opcional: si tiene cuenta, le avisamos) y una dedicatoria. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftRequestDTO {
    private String recipientName;
    private String recipientEmail;
    private String message;
}
