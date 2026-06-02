package com.devrenno.bookland.orders.domain.exception;

public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String reason) {
        super("Payment declined: " + (reason != null ? reason : "no reason provided"));
    }
}
