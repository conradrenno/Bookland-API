package com.devrenno.bookland.orders.infrastructure.config;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.VerifyPurchaseUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.application.service.AddCartItemService;
import com.devrenno.bookland.orders.application.service.VerifyPurchaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the orders module. Exposes the internal OrdersController (HTTP delivery)
 * plus the cross-module boundary use cases: VerifyPurchaseUseCase (consumed by reviews) and
 * AddCartItemUseCase (consumed by wishlist's move-to-cart).
 */
@Configuration
public class OrderBeansConfig {

    @Bean
    public OrdersController ordersController(CartPersistencePort cartPersistencePort,
                                             OrderPersistencePort orderPersistencePort,
                                             BookInfoPort bookInfoPort, BookStockPort bookStockPort,
                                             PaymentPort paymentPort, RefundPort refundPort,
                                             TransactionPort transactionPort) {
        return OrdersController.create(cartPersistencePort, orderPersistencePort, bookInfoPort,
                bookStockPort, paymentPort, refundPort, transactionPort);
    }

    @Bean
    public VerifyPurchaseUseCase verifyPurchaseUseCase(PurchaseVerificationPort purchaseVerificationPort) {
        return VerifyPurchaseService.create(purchaseVerificationPort);
    }

    @Bean
    public AddCartItemUseCase addCartItemUseCase(CartPersistencePort cartPersistencePort,
                                                 BookInfoPort bookInfoPort) {
        return AddCartItemService.create(cartPersistencePort, bookInfoPort);
    }
}
