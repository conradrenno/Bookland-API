package com.devrenno.bookland.payments.application.dto;

public record PaymentResult(
        boolean approved,
        String transactionId,
        String declineReason
) {}
