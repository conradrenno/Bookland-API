package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.dto.WishlistView;
import com.devrenno.bookland.wishlist.application.port.in.RemoveWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;

import java.util.UUID;

public class RemoveWishlistItemService implements RemoveWishlistItemUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;

    private RemoveWishlistItemService(WishlistPersistencePort wishlistPersistencePort,
                                      WishlistBookInfoPort wishlistBookInfoPort) {
        this.wishlistPersistencePort = wishlistPersistencePort;
        this.wishlistBookInfoPort = wishlistBookInfoPort;
    }

    public static RemoveWishlistItemService create(WishlistPersistencePort wishlistPersistencePort,
                                                   WishlistBookInfoPort wishlistBookInfoPort) {
        return new RemoveWishlistItemService(wishlistPersistencePort, wishlistBookInfoPort);
    }

    @Override
    public WishlistView execute(UUID customerId, UUID bookId) {
        Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new WishlistItemNotFoundException(customerId, bookId));
        wishlist.removeItem(bookId);
        Wishlist saved = wishlistPersistencePort.save(wishlist);
        return WishlistViewAssembler.toView(saved, wishlistBookInfoPort);
    }
}
