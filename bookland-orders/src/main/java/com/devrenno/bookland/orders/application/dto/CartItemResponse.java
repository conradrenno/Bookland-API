package com.devrenno.bookland.orders.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(UUID bookId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
