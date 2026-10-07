package com.devrenno.bookland.orders.infrastructure.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdersInboxJpaRepository extends JpaRepository<OrdersInboxJpaEntity, UUID> {
}
