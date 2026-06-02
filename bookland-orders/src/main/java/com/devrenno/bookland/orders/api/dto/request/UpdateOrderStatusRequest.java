package com.devrenno.bookland.orders.api.dto.request;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus newStatus,
        UUID adminId
) {}
