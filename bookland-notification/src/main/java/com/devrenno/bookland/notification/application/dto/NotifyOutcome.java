package com.devrenno.bookland.notification.application.dto;

public enum NotifyOutcome {
    /** The email was written and handed to the task queue, which confirmed it. */
    QUEUED,
    /** No email address on record for the order: nothing to send. */
    NO_RECIPIENT
}
