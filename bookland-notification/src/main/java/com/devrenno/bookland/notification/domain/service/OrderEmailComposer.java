package com.devrenno.bookland.notification.domain.service;

import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import com.devrenno.bookland.notification.domain.valueobject.OrderLine;
import com.devrenno.bookland.notification.domain.valueobject.OrderNotice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Writes the email for an order event: plain text, one per kind (decision 6 of step 6 — HTML is
 * cosmetic and can come later). An order with no email address on record gets none: there is
 * nobody to write to, and nowhere to look one up.
 */
public class OrderEmailComposer {

    public Optional<EmailMessage> compose(OrderNotice notice) {
        if (notice.customerEmail() == null || notice.customerEmail().isBlank()) {
            return Optional.empty();
        }
        String reference = shortReference(notice);
        String subject;
        String message;
        switch (notice.kind()) {
            case CONFIRMED -> {
                subject = "Your Bookland order " + reference + " is confirmed";
                message = "Thank you for your order. Your payment was approved and your books are reserved.";
            }
            case PAYMENT_FAILED -> {
                subject = "Payment for your Bookland order " + reference + " was not approved";
                message = "We could not charge your order, so it was not placed and nothing was taken from you."
                        + reasonLine(notice)
                        + "\nYour cart is still there if you want to try again.";
            }
            case REJECTED -> {
                subject = "Your Bookland order " + reference + " could not be placed";
                message = "Some of the books in your order ran out before we could reserve them, so the order"
                        + " was not placed and you were not charged."
                        + reasonLine(notice)
                        + "\nYour cart is still there if you want to try again.";
            }
            case SHIPPED -> {
                subject = "Your Bookland order " + reference + " is on its way";
                message = "Good news: your order has been shipped.";
            }
            case CANCELLED -> {
                subject = "Your Bookland order " + reference + " was cancelled";
                message = "Your order was cancelled. The amount paid will be refunded to you.";
            }
            default -> throw new IllegalStateException("No email for " + notice.kind());
        }
        String body = greeting(notice) + "\n\n" + message + "\n\n" + summary(notice)
                + "\n\nThe Bookland team\n";
        return Optional.of(new EmailMessage(notice.customerEmail(), subject, body));
    }

    private static String shortReference(OrderNotice notice) {
        return "#" + notice.orderId().toString().substring(0, 8);
    }

    private static String greeting(OrderNotice notice) {
        String name = notice.customerName();
        return name == null || name.isBlank() ? "Hello," : "Hello " + name + ",";
    }

    private static String reasonLine(OrderNotice notice) {
        return notice.reason() == null || notice.reason().isBlank() ? "" : "\nReason: " + notice.reason();
    }

    private static String summary(OrderNotice notice) {
        StringBuilder text = new StringBuilder("Order " + notice.orderId() + "\n");
        for (OrderLine line : notice.lines()) {
            text.append("  ").append(line.quantity()).append(" x ").append(line.title())
                    .append(" — ").append(money(line.unitPrice())).append('\n');
        }
        if (notice.totalAmount() != null) {
            text.append("Total: ").append(money(notice.totalAmount()));
        }
        return text.toString().stripTrailing();
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "-" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
