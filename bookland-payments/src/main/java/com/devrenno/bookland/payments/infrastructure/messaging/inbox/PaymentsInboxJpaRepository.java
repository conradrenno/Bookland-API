package com.devrenno.bookland.payments.infrastructure.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentsInboxJpaRepository extends JpaRepository<PaymentsInboxJpaEntity, UUID> {
}
