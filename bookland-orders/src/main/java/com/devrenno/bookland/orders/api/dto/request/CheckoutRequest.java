package com.devrenno.bookland.orders.api.dto.request;

import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull PaymentMethod paymentMethod) {}
