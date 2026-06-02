package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.annotation.UseCase;
import com.devrenno.bookland.wishlist.application.dto.WishlistResponse;
import com.devrenno.bookland.wishlist.application.port.in.RemoveWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RemoveWishlistItemService implements RemoveWishlistItemUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;

    @Override
    public WishlistResponse execute(UUID customerId, UUID bookId) {
        Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new WishlistItemNotFoundException(customerId, bookId));
        wishlist.removeItem(bookId);
        Wishlist saved = wishlistPersistencePort.save(wishlist);
        return WishlistResponseMapper.toResponse(saved, wishlistBookInfoPort);
    }
}
