package com.devrenno.bookland.orders.application.dto;

import java.util.UUID;

/** So many units of one book, as the checkout asks the catalog to reserve them. */
public record StockLine(UUID bookId, int quantity) {
}
