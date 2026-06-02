package com.devrenno.bookland.inventory.application.dto;

import java.util.UUID;

public record LowStockBookInfo(UUID id, String title, String isbn, int stockQuantity) {}
