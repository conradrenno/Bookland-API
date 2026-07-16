package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.port.in.MoveToCartUseCase;
import com.devrenno.bookland.wishlist.application.port.out.CartAddPort;
import com.devrenno.bookland.wishlist.application.port.out.TransactionPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;

import java.util.UUID;

public class MoveToCartService implements MoveToCartUseCase {

    private final WishlistPersistencePort wishlistPersistencePort;
    private final WishlistBookInfoPort wishlistBookInfoPort;
    private final CartAddPort cartAddPort;
    private final TransactionPort transactionPort;

    private MoveToCartService(WishlistPersistencePort wishlistPersistencePort,
                              WishlistBookInfoPort wishlistBookInfoPort,
                              CartAddPort cartAddPort,
                              TransactionPort transactionPort) {
        this.wishlistPersistencePort = wishlistPersistencePort;
        this.wishlistBookInfoPort = wishlistBookInfoPort;
        this.cartAddPort = cartAddPort;
        this.transactionPort = transactionPort;
    }

    public static MoveToCartService create(WishlistPersistencePort wishlistPersistencePort,
                                           WishlistBookInfoPort wishlistBookInfoPort,
                                           CartAddPort cartAddPort,
                                           TransactionPort transactionPort) {
        return new MoveToCartService(wishlistPersistencePort, wishlistBookInfoPort, cartAddPort, transactionPort);
    }

    @Override
    public void execute(UUID customerId, UUID bookId) {
        transactionPort.inTransaction(() -> {
            Wishlist wishlist = wishlistPersistencePort.findByCustomerId(customerId)
                    .orElseThrow(() -> new WishlistItemNotFoundException(customerId, bookId));

            wishlistBookInfoPort.getBookInfo(bookId);

            cartAddPort.addToCart(customerId, bookId, 1);
            wishlist.removeItem(bookId);
            wishlistPersistencePort.save(wishlist);
        });
    }
}
