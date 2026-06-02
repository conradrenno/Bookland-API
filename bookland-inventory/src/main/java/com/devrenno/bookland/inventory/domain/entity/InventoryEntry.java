package com.devrenno.bookland.inventory.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InventoryEntry {

    private final UUID id;
    private final UUID bookId;
    private final int previousQuantity;
    private final int newQuantity;
    private final int delta;
    private final String reason;
    private final UUID adjustedBy;
    private final LocalDateTime adjustedAt;

    public static InventoryEntry create(UUID bookId, int previousQty, int newQty, String reason, UUID adjustedBy) {
        return InventoryEntry.builder()
                .id(UUID.randomUUID())
                .bookId(bookId)
                .previousQuantity(previousQty)
                .newQuantity(newQty)
                .delta(newQty - previousQty)
                .reason(reason)
                .adjustedBy(adjustedBy)
                .adjustedAt(LocalDateTime.now())
                .build();
    }
}
