package com.devrenno.bookland.catalog.domain.entity;

import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The units an order took from the catalog, recorded so they can be given back exactly once.
 *
 * <p>One per order, keyed by the order id — which is what makes reserving and releasing idempotent:
 * a repeated request finds the record and answers from it instead of touching stock again. That
 * matters because the requests will arrive as messages, and a message can be delivered twice.
 *
 * <p>A reservation that could not be made is recorded too ({@link Status#FAILED}, no items). Without
 * it, a duplicate request arriving after the failure would find nothing, try again, and might
 * succeed — leaving units reserved for an order that was already rejected.
 */
@Getter
public class StockReservation {

    public enum Status { RESERVED, RELEASED, FAILED }

    /** One line of the reservation: so many units of one book. */
    public record Item(UUID bookId, int quantity) {
        public Item {
            Objects.requireNonNull(bookId, "bookId");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive: " + quantity);
            }
        }
    }

    private final UUID orderId;
    private final List<Item> items;
    private Status status;
    private final Instant createdAt;
    private Instant updatedAt;

    private StockReservation(UUID orderId, List<Item> items, Status status, Instant createdAt, Instant updatedAt) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.items = List.copyOf(items);
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StockReservation reserved(UUID orderId, List<Item> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("a reservation needs at least one item");
        }
        Instant now = Instant.now();
        return new StockReservation(orderId, items, Status.RESERVED, now, now);
    }

    public static StockReservation failed(UUID orderId) {
        Instant now = Instant.now();
        return new StockReservation(orderId, List.of(), Status.FAILED, now, now);
    }

    public static StockReservation reconstitute(UUID orderId, List<Item> items, Status status,
                                                Instant createdAt, Instant updatedAt) {
        return new StockReservation(orderId, items, status, createdAt, updatedAt);
    }

    /**
     * Marks the units as given back. Only a held reservation can be released; the caller returns the
     * units to stock when, and only when, this answers true.
     */
    public boolean release() {
        if (status != Status.RESERVED) {
            return false;
        }
        status = Status.RELEASED;
        updatedAt = Instant.now();
        return true;
    }
}
