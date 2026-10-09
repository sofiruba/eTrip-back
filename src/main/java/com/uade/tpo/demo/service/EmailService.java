package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.Order;

public interface EmailService {
    void sendOrderConfirmation(Order order);
}
