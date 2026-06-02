package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.annotation.UseCase;
import com.devrenno.bookland.wishlist.application.dto.WishlistResponse;
import com.devrenno.bookland.wishlist.application.port.in.AddWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class AddWishlistItemService implements AddWishlistItemUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;

    @Override
    public WishlistResponse execute(UUID customerId, UUID bookId) {
        wishlistBookInfoPort.getBookInfo(bookId);

        Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                .orElseGet(() -> Wishlist.createFor(customerId));
        wishlist.addItem(bookId);
        Wishlist saved = wishlistPersistencePort.save(wishlist);
        return WishlistResponseMapper.toResponse(saved, wishlistBookInfoPort);
    }
}
