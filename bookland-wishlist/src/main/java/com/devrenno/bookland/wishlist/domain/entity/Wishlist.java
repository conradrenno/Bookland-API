package com.devrenno.bookland.wishlist.domain.entity;

import com.devrenno.bookland.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class Wishlist {

    private final UUID id;
    private final UUID customerId;
    @Builder.Default
    private List<WishlistItem> items = new ArrayList<>();

    public static Wishlist createFor(UUID customerId) {
        return Wishlist.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .items(new ArrayList<>())
                .build();
    }

    public void addItem(UUID bookId) {
        boolean exists = items.stream().anyMatch(i -> i.getBookId().equals(bookId));
        if (exists) throw new WishlistItemAlreadyExistsException(customerId, bookId);
        items.add(new WishlistItem(bookId, LocalDateTime.now()));
    }

    public void removeItem(UUID bookId) {
        boolean removed = items.removeIf(i -> i.getBookId().equals(bookId));
        if (!removed) throw new WishlistItemNotFoundException(customerId, bookId);
    }
}
