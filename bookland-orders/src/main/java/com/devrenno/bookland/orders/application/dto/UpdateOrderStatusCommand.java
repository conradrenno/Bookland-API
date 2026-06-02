package com.devrenno.bookland.orders.application.dto;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.util.UUID;

public record UpdateOrderStatusCommand(UUID orderId, OrderStatus newStatus, UUID adminId) {}
