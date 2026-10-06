package com.devrenno.bookland.orders.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Query read-model: a cart whose items carry the catalog data needed to render them.
 *
 * <p>{@code id} and {@code updatedAt} are null while the customer has no cart: one is created by the
 * first item added, not by looking at it.
 */
public record CartView(
        UUID id,
        UUID customerId,
        List<CartItemView> items,
        Instant updatedAt
) {

    /**
     * The answer for a customer who has never added anything. Reading does not create a cart: a GET
     * that wrote would race against itself, since a customer has at most one cart. The previous
     * answer, an unsaved {@code Cart.createFor}, reported an id and a timestamp that existed nowhere
     * and changed on every call.
     */
    public static CartView emptyFor(UUID customerId) {
        return new CartView(null, customerId, List.of(), null);
    }
}
