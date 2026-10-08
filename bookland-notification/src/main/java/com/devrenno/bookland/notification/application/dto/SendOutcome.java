package com.devrenno.bookland.notification.application.dto;

public enum SendOutcome {
    /** Handed to the mail server and recorded. */
    SENT,
    /** This email (same order, same kind) was sent before: a duplicate task, not sent again. */
    ALREADY_SENT
}
