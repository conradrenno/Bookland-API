package com.devrenno.bookland.orders.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID bookId, String title, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
