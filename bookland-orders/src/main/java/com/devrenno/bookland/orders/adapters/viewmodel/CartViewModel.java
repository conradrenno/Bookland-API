package com.devrenno.bookland.orders.adapters.viewmodel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code id} and {@code updatedAt} are null while the customer has no cart yet (see {@code CartView}). */
public record CartViewModel(
        UUID id,
        UUID customerId,
        List<CartItemViewModel> items,
        BigDecimal total,
        Instant updatedAt
) {}
