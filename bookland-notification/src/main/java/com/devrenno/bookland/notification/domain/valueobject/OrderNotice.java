package com.devrenno.bookland.notification.domain.valueobject;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What happened to an order, as far as telling the customer goes — everything the email needs, as
 * the order's event carried it. Nothing here is looked up: orders stored the customer's email and
 * name at checkout so this service never asks anyone.
 *
 * @param customerEmail null for orders placed before orders stored it; such an order gets no email
 * @param customerName  null when the customer's token carried none
 * @param reason        why the checkout failed (PAYMENT_FAILED, REJECTED); null otherwise
 */
public record OrderNotice(UUID orderId, OrderEventKind kind, String customerEmail, String customerName,
                          BigDecimal totalAmount, List<OrderLine> lines, String reason) {

    public OrderNotice {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
