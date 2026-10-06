package com.devrenno.bookland.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationJpaEntity {

    /** The order's id: one reservation per order, which is what makes reserving idempotent. */
    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stock_reservation_items", joinColumns = @JoinColumn(name = "order_id"))
    @Builder.Default
    private List<StockReservationItemEmbeddable> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
