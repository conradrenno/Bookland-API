package com.devrenno.bookland.orders.application.dto;

import java.util.UUID;

public record UpdateCartItemCommand(UUID customerId, UUID bookId, int quantity) {}
