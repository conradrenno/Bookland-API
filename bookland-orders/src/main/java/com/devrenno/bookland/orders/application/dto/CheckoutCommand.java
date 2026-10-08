package com.devrenno.bookland.orders.application.dto;

import com.devrenno.bookland.orders.domain.entity.PaymentMethod;

import java.util.UUID;

/** The customer's email and name come from the caller's access token and are stored on the order. */
public record CheckoutCommand(UUID customerId, String customerEmail, String customerName,
                              PaymentMethod paymentMethod) {}
