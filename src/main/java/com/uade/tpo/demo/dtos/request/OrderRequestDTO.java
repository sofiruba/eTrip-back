package com.uade.tpo.demo.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    private String couponCode;
    /** Medio de pago simulado: "card" (tarjeta nueva), "saved" (tarjeta guardada) o una billetera (ej. "mercadopago"). */
    private String paymentMethod;
    /** Solo con paymentMethod = "card": el número de la tarjeta (se valida y no se guarda). */
    private String cardNumber;
}
