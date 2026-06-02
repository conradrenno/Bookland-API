package com.devrenno.bookland.orders.application.dto;

import java.util.UUID;

public record AddCartItemCommand(UUID customerId, UUID bookId, int quantity) {}
