package com.devrenno.bookland.notification.domain.valueobject;

/** The order events the customer is told about. Every other event on the topic is not this service's concern. */
public enum OrderEventKind {
    CONFIRMED,
    PAYMENT_FAILED,
    REJECTED,
    SHIPPED,
    CANCELLED
}
