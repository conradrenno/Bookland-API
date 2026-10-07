package com.devrenno.bookland.orders.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrdersOutboxJpaRepository extends JpaRepository<OrdersOutboxJpaEntity, UUID> {

    /** The relay's batch: pending messages, oldest first, so one order's messages leave in the order they were written. */
    List<OrdersOutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
