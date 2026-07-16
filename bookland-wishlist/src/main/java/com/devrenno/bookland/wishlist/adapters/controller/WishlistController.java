package com.devrenno.bookland.wishlist.adapters.controller;

import com.devrenno.bookland.wishlist.adapters.presenter.WishlistPresenter;
import com.devrenno.bookland.wishlist.adapters.viewmodel.WishlistViewModel;
import com.devrenno.bookland.wishlist.application.port.in.AddWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.in.GetWishlistUseCase;
import com.devrenno.bookland.wishlist.application.port.in.MoveToCartUseCase;
import com.devrenno.bookland.wishlist.application.port.in.RemoveWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.out.CartAddPort;
import com.devrenno.bookland.wishlist.application.port.out.TransactionPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.application.service.AddWishlistItemService;
import com.devrenno.bookland.wishlist.application.service.GetWishlistService;
import com.devrenno.bookland.wishlist.application.service.MoveToCartService;
import com.devrenno.bookland.wishlist.application.service.RemoveWishlistItemService;

import java.util.UUID;

/**
 * Internal controller: orchestrates the wishlist use cases and delegates to the Presenter.
 * Also the module's composition root — its create(...) factory wires the use cases from the
 * outbound ports it receives (as interfaces). Framework-free.
 */
public class WishlistController {

    private final GetWishlistUseCase getWishlistUseCase;
    private final AddWishlistItemUseCase addWishlistItemUseCase;
    private final RemoveWishlistItemUseCase removeWishlistItemUseCase;
    private final MoveToCartUseCase moveToCartUseCase;
    private final WishlistPresenter presenter;

    private WishlistController(GetWishlistUseCase getWishlistUseCase,
                             AddWishlistItemUseCase addWishlistItemUseCase,
                             RemoveWishlistItemUseCase removeWishlistItemUseCase,
                             MoveToCartUseCase moveToCartUseCase,
                             WishlistPresenter presenter) {
        this.getWishlistUseCase = getWishlistUseCase;
        this.addWishlistItemUseCase = addWishlistItemUseCase;
        this.removeWishlistItemUseCase = removeWishlistItemUseCase;
        this.moveToCartUseCase = moveToCartUseCase;
        this.presenter = presenter;
    }

    public static WishlistController create(WishlistPersistencePort wishlistPersistencePort,
                                            WishlistBookInfoPort wishlistBookInfoPort,
                                            CartAddPort cartAddPort,
                                            TransactionPort transactionPort) {
        return new WishlistController(
                GetWishlistService.create(wishlistPersistencePort, wishlistBookInfoPort),
                AddWishlistItemService.create(wishlistPersistencePort, wishlistBookInfoPort),
                RemoveWishlistItemService.create(wishlistPersistencePort, wishlistBookInfoPort),
                MoveToCartService.create(wishlistPersistencePort, wishlistBookInfoPort, cartAddPort, transactionPort),
                WishlistPresenter.create()
        );
    }

    public WishlistViewModel get(UUID customerId) {
        return presenter.present(getWishlistUseCase.execute(customerId));
    }

    public WishlistViewModel addItem(UUID customerId, UUID bookId) {
        return presenter.present(addWishlistItemUseCase.execute(customerId, bookId));
    }

    public WishlistViewModel removeItem(UUID customerId, UUID bookId) {
        return presenter.present(removeWishlistItemUseCase.execute(customerId, bookId));
    }

    public void moveToCart(UUID customerId, UUID bookId) {
        moveToCartUseCase.execute(customerId, bookId);
    }
}
