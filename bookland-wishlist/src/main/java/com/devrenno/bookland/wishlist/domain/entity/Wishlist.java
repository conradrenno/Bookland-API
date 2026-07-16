package com.devrenno.bookland.wishlist.domain.entity;

import com.devrenno.bookland.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Wishlist {

    private final UUID id;
    private final UUID customerId;
    private final List<WishlistItem> items;

    private Wishlist(UUID id, UUID customerId, List<WishlistItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
    }

    public static Wishlist createFor(UUID customerId) {
        return new Wishlist(UUID.randomUUID(), customerId, new ArrayList<>());
    }

    public static Wishlist reconstitute(UUID id, UUID customerId, List<WishlistItem> items) {
        return new Wishlist(id, customerId, new ArrayList<>(items));
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
