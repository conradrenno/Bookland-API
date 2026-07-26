package com.devrenno.bookland.orders.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookInfo(UUID id, String title, String coverImageUrl, BigDecimal price, int stockQuantity) {}
