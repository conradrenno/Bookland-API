package com.devrenno.bookland.orders.infrastructure.config;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.CheckActiveOrdersUseCase;
import com.devrenno.bookland.orders.application.port.in.CheckoutSagaUseCase;
import com.devrenno.bookland.orders.application.port.in.VerifyPurchaseUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.CheckoutCommandPort;
import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.application.service.AddCartItemService;
import com.devrenno.bookland.orders.application.service.CheckActiveOrdersService;
import com.devrenno.bookland.orders.application.service.CheckoutSagaService;
import com.devrenno.bookland.orders.application.service.VerifyPurchaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the orders module. Exposes the internal OrdersController (HTTP delivery)
 * plus the cross-module boundary use cases: VerifyPurchaseUseCase (consumed by reviews),
 * AddCartItemUseCase (consumed by wishlist's move-to-cart) and CheckActiveOrdersUseCase
 * (served over gRPC to the catalog by OrderActivityGrpcService).
 */
@Configuration
public class OrderBeansConfig {

    @Bean
    public OrdersController ordersController(CartPersistencePort cartPersistencePort,
                                             OrderPersistencePort orderPersistencePort,
                                             BookInfoPort bookInfoPort, CheckoutCommandPort checkoutCommandPort,
                                             OrderEventPort orderEventPort,
                                             TransactionPort transactionPort) {
        return OrdersController.create(cartPersistencePort, orderPersistencePort, bookInfoPort,
                checkoutCommandPort, orderEventPort, transactionPort);
    }

    /** The orchestrator's reactions to the saga's replies, consumed by this module's own reply listener. */
    @Bean
    public CheckoutSagaUseCase checkoutSagaUseCase(OrderPersistencePort orderPersistencePort,
                                                   CartPersistencePort cartPersistencePort,
                                                   CheckoutCommandPort checkoutCommandPort,
                                                   OrderEventPort orderEventPort,
                                                   TransactionPort transactionPort) {
        return CheckoutSagaService.create(orderPersistencePort, cartPersistencePort, checkoutCommandPort,
                orderEventPort, transactionPort);
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

    @Bean
    public CheckActiveOrdersUseCase checkActiveOrdersUseCase(OrderPersistencePort orderPersistencePort) {
        return CheckActiveOrdersService.create(orderPersistencePort);
    }
}
