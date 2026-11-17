package com.uade.tpo.demo.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** La compra es un regalo: para quién es (obligatorio), su email (opcional: se le avisa por mail y se le entrega cuando tenga una cuenta verificada con ese email) y una dedicatoria. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftRequestDTO {
    private String recipientName;
    private String recipientEmail;
    private String message;
}
