package com.devrenno.bookland.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** An email the notification service has sent, by key — and the history of what went to whom. */
@Entity
@Table(name = "sent_emails")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SentEmailJpaEntity {

    /** {@code <orderId>:<KIND>}: one email per order and kind. */
    @Id
    @Column(name = "email_key", nullable = false, updatable = false, length = 100)
    private String emailKey;

    @Column(nullable = false, updatable = false)
    private String recipient;

    @Column(nullable = false, updatable = false, length = 500)
    private String subject;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;
}
