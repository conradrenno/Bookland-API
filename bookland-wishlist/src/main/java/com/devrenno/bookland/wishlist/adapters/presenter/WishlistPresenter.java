package com.devrenno.bookland.wishlist.adapters.presenter;

import com.devrenno.bookland.wishlist.adapters.viewmodel.WishlistItemViewModel;
import com.devrenno.bookland.wishlist.adapters.viewmodel.WishlistViewModel;
import com.devrenno.bookland.wishlist.application.dto.WishlistView;

/**
 * Transforms the WishlistView read-model into the delivery-facing WishlistViewModel. Plain Java.
 */
public class WishlistPresenter {

    private WishlistPresenter() {
    }

    public static WishlistPresenter create() {
        return new WishlistPresenter();
    }

    public WishlistViewModel present(WishlistView view) {
        return new WishlistViewModel(
                view.customerId(),
                view.items().stream()
                        .map(i -> new WishlistItemViewModel(
                                i.bookId(), i.title(), i.price(),
                                i.stockQuantity(), i.available(), i.addedAt()
                        ))
                        .toList()
        );
    }
}
