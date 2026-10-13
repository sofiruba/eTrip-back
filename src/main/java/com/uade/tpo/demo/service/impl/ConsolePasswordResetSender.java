package com.uade.tpo.demo.service.impl;

import org.springframework.stereotype.Component;

import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.service.PasswordResetSender;

// Versión de desarrollo: imprime el código en la consola del back (no hay mail configurado).
@Component
public class ConsolePasswordResetSender implements PasswordResetSender {

    @Override
    public void send(User user, String code) {
        System.out.println("==============================================");
        System.out.println(" Código para recuperar la contraseña");
        System.out.println(" Usuario: " + user.getEmail());
        System.out.println(" Código:  " + code + " (vence en 15 minutos)");
        System.out.println("==============================================");
    }
}
