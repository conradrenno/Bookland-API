package com.devrenno.bookland.orders.application.dto;

/**
 * What the payment step answered, in the orders module's own terms.
 *
 * @param approved      whether the charge went through
 * @param declineReason why it did not, when it did not
 */
public record PaymentOutcome(boolean approved, String declineReason) {
}
