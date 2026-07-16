package com.devrenno.bookland.orders.infrastructure.web.dto;

import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull PaymentMethod paymentMethod) {}
