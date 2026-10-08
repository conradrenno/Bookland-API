package com.devrenno.bookland.notification.infrastructure.messaging.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** An order event the notification service has already turned into an email task. */
@Entity
@Table(name = "notification_inbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInboxJpaEntity {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
