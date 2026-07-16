package com.devrenno.bookland.orders.adapters.viewmodel;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemViewModel(UUID bookId, String title, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
