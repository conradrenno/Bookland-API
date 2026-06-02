package com.devrenno.bookland.wishlist.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddWishlistItemRequest(@NotNull UUID bookId) {}
