package com.devrenno.bookland.notification.domain.valueobject;

/** A plain-text email, ready to send. */
public record EmailMessage(String to, String subject, String body) {
}
