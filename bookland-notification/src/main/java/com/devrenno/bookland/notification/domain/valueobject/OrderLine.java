package com.devrenno.bookland.notification.domain.valueobject;

import java.math.BigDecimal;

public record OrderLine(String title, int quantity, BigDecimal unitPrice) {
}
