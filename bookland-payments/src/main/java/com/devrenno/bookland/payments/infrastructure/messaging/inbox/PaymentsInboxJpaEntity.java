package com.devrenno.bookland.payments.infrastructure.messaging.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A message the payments module has already consumed. */
@Entity
@Table(name = "payments_inbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsInboxJpaEntity {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
