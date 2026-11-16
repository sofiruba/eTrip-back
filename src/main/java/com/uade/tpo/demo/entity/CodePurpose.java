package com.uade.tpo.demo.entity;

/** Para qué sirve un código de 6 dígitos que mandamos por mail. */
public enum CodePurpose {
    /** Confirmar que el email del registro es de quien se registró. */
    VERIFY_EMAIL,
    /** "Olvidé mi contraseña". */
    RESET_PASSWORD
}
