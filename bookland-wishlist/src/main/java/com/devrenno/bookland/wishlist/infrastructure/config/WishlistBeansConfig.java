package com.devrenno.bookland.wishlist.infrastructure.config;

import com.devrenno.bookland.wishlist.adapters.controller.WishlistController;
import com.devrenno.bookland.wishlist.application.port.out.CartAddPort;
import com.devrenno.bookland.wishlist.application.port.out.TransactionPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the wishlist module. Builds the framework-free inner graph from the outbound
 * ports (implemented by Spring adapters) and exposes the internal WishlistController as a bean.
 */
@Configuration
public class WishlistBeansConfig {

    @Bean
    public WishlistController wishlistController(WishlistPersistencePort wishlistPersistencePort,
                                                 WishlistBookInfoPort wishlistBookInfoPort,
                                                 CartAddPort cartAddPort,
                                                 TransactionPort transactionPort) {
        return WishlistController.create(wishlistPersistencePort, wishlistBookInfoPort, cartAddPort, transactionPort);
    }
}
