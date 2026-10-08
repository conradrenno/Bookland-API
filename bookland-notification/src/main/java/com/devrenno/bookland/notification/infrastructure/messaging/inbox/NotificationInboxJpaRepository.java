package com.devrenno.bookland.notification.infrastructure.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationInboxJpaRepository extends JpaRepository<NotificationInboxJpaEntity, UUID> {
}
