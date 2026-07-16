package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.dto.WishlistView;
import com.devrenno.bookland.wishlist.application.port.in.AddWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;

import java.util.UUID;

public class AddWishlistItemService implements AddWishlistItemUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;

    private AddWishlistItemService(WishlistPersistencePort wishlistPersistencePort,
                                   WishlistBookInfoPort wishlistBookInfoPort) {
        this.wishlistPersistencePort = wishlistPersistencePort;
        this.wishlistBookInfoPort = wishlistBookInfoPort;
    }

    public static AddWishlistItemService create(WishlistPersistencePort wishlistPersistencePort,
                                                WishlistBookInfoPort wishlistBookInfoPort) {
        return new AddWishlistItemService(wishlistPersistencePort, wishlistBookInfoPort);
    }

    @Override
    public WishlistView execute(UUID customerId, UUID bookId) {
        wishlistBookInfoPort.getBookInfo(bookId);

        Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                .orElseGet(() -> Wishlist.createFor(customerId));
        wishlist.addItem(bookId);
        Wishlist saved = wishlistPersistencePort.save(wishlist);
        return WishlistViewAssembler.toView(saved, wishlistBookInfoPort);
    }
}
