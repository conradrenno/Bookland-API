package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.annotation.UseCase;
import com.devrenno.bookland.wishlist.application.port.in.MoveToCartUseCase;
import com.devrenno.bookland.wishlist.application.port.out.CartAddPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class MoveToCartService implements MoveToCartUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;
    private final CartAddPort cartAddPort;

    @Override
    @Transactional
    public void execute(UUID customerId, UUID bookId) {
        Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new WishlistItemNotFoundException(customerId, bookId));

        wishlistBookInfoPort.getBookInfo(bookId);

        cartAddPort.addToCart(customerId, bookId, 1);
        wishlist.removeItem(bookId);
        wishlistPersistencePort.save(wishlist);
    }
}
