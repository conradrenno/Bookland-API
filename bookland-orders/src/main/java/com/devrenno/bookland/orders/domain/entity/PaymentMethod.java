package com.devrenno.bookland.orders.domain.entity;

/**
 * How the customer chose to pay — the orders module's own type, not the payments module's.
 *
 * <p>It used to be {@code payments.domain.entity.PaymentMethod}, imported into this module's
 * use cases, ports and controller: a class of another service in the inner layers, which the day
 * payments runs as a process of its own would not even be on the classpath. The two enums carry the
 * same values; the adapter that talks to payments translates by name.
 */
public enum PaymentMethod {
    CREDIT_CARD, DEBIT_CARD, PAYPAL, PIX
}
