package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.User;

// Cómo le llega al usuario el código para recuperar la contraseña.
// Hoy solo existe ConsolePasswordResetSender; para mandarlo por mail alcanza con
// otra implementación de esta interfaz (y sacarle @Component a la de consola).
public interface PasswordResetSender {
    void send(User user, String code);
}
