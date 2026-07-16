package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.dto.WishlistView;
import com.devrenno.bookland.wishlist.application.port.in.GetWishlistUseCase;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;

import java.util.UUID;

public class GetWishlistService implements GetWishlistUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;

    private GetWishlistService(WishlistPersistencePort wishlistPersistencePort,
                               WishlistBookInfoPort wishlistBookInfoPort) {
        this.wishlistPersistencePort = wishlistPersistencePort;
        this.wishlistBookInfoPort = wishlistBookInfoPort;
    }

    public static GetWishlistService create(WishlistPersistencePort wishlistPersistencePort,
                                            WishlistBookInfoPort wishlistBookInfoPort) {
        return new GetWishlistService(wishlistPersistencePort, wishlistBookInfoPort);
    }

    @Override
    public WishlistView execute(UUID customerId) {
        Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                .orElseGet(() -> Wishlist.createFor(customerId));
        return WishlistViewAssembler.toView(wishlist, wishlistBookInfoPort);
    }
}
