package com.devrenno.bookland.wishlist.adapters.viewmodel;

import java.util.List;
import java.util.UUID;

public record WishlistViewModel(
        UUID customerId,
        List<WishlistItemViewModel> items
) {}
