package com.devrenno.bookland.orders.infrastructure.web.dto;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Who made the change is the caller, from the access token — never a field the caller fills in. */
public record UpdateOrderStatusRequest(
        @NotNull OrderStatus newStatus
) {}
