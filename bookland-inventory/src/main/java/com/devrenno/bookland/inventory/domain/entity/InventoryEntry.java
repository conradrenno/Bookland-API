package com.devrenno.bookland.inventory.domain.entity;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class InventoryEntry {

    private final UUID id;
    private final UUID bookId;
    private final int previousQuantity;
    private final int newQuantity;
    private final int delta;
    private final String reason;
    private final UUID adjustedBy;
    private final Instant adjustedAt;

    private InventoryEntry(UUID id, UUID bookId, int previousQuantity, int newQuantity, int delta,
                          String reason, UUID adjustedBy, Instant adjustedAt) {
        this.id = id;
        this.bookId = bookId;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.delta = delta;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt;
    }

    /** Records a new stock adjustment; delta is derived from the quantities. */
    public static InventoryEntry create(UUID bookId, int previousQty, int newQty, String reason, UUID adjustedBy) {
        return new InventoryEntry(
                UUID.randomUUID(),
                bookId,
                previousQty,
                newQty,
                newQty - previousQty,
                reason,
                adjustedBy,
                Instant.now()
        );
    }

    /** Rehydrates an entry from persisted state. */
    public static InventoryEntry reconstitute(UUID id, UUID bookId, int previousQuantity, int newQuantity, int delta,
                                              String reason, UUID adjustedBy, Instant adjustedAt) {
        return new InventoryEntry(id, bookId, previousQuantity, newQuantity, delta, reason, adjustedBy, adjustedAt);
    }
}
