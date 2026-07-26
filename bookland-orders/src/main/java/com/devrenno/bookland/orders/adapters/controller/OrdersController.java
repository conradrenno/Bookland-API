package com.devrenno.bookland.orders.adapters.controller;

import com.devrenno.bookland.orders.adapters.presenter.OrderPresenter;
import com.devrenno.bookland.orders.adapters.viewmodel.CartViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;
import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.CancelOrderUseCase;
import com.devrenno.bookland.orders.application.port.in.CheckoutUseCase;
import com.devrenno.bookland.orders.application.port.in.GetCartUseCase;
import com.devrenno.bookland.orders.application.port.in.GetOrderByIdUseCase;
import com.devrenno.bookland.orders.application.port.in.GetOrderHistoryUseCase;
import com.devrenno.bookland.orders.application.port.in.RemoveCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.UpdateCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.UpdateOrderStatusUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.application.service.AddCartItemService;
import com.devrenno.bookland.orders.application.service.CancelOrderService;
import com.devrenno.bookland.orders.application.service.CheckoutService;
import com.devrenno.bookland.orders.application.service.GetCartService;
import com.devrenno.bookland.orders.application.service.GetOrderByIdService;
import com.devrenno.bookland.orders.application.service.GetOrderHistoryService;
import com.devrenno.bookland.orders.application.service.RemoveCartItemService;
import com.devrenno.bookland.orders.application.service.UpdateCartItemService;
import com.devrenno.bookland.orders.application.service.UpdateOrderStatusService;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;

import java.util.UUID;

/**
 * Internal controller: orchestrates the orders HTTP-facing use cases and delegates to the Presenter.
 * Also the module's composition root for HTTP delivery. Framework-free.
 * (The cross-module VerifyPurchaseUseCase — consumed by reviews — is exposed as its own bean.)
 */
public class OrdersController {

    private final GetCartUseCase getCartUseCase;
    private final AddCartItemUseCase addCartItemUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final CheckoutUseCase checkoutUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final GetOrderHistoryUseCase getOrderHistoryUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderPresenter presenter;

    private OrdersController(GetCartUseCase getCartUseCase, AddCartItemUseCase addCartItemUseCase,
                             UpdateCartItemUseCase updateCartItemUseCase, RemoveCartItemUseCase removeCartItemUseCase,
                             CheckoutUseCase checkoutUseCase, GetOrderByIdUseCase getOrderByIdUseCase,
                             GetOrderHistoryUseCase getOrderHistoryUseCase, CancelOrderUseCase cancelOrderUseCase,
                             UpdateOrderStatusUseCase updateOrderStatusUseCase, OrderPresenter presenter) {
        this.getCartUseCase = getCartUseCase;
        this.addCartItemUseCase = addCartItemUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeCartItemUseCase = removeCartItemUseCase;
        this.checkoutUseCase = checkoutUseCase;
        this.getOrderByIdUseCase = getOrderByIdUseCase;
        this.getOrderHistoryUseCase = getOrderHistoryUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.presenter = presenter;
    }

    public static OrdersController create(CartPersistencePort cartPersistencePort,
                                          OrderPersistencePort orderPersistencePort,
                                          BookInfoPort bookInfoPort, BookStockPort bookStockPort,
                                          PaymentPort paymentPort, RefundPort refundPort,
                                          TransactionPort transactionPort) {
        return new OrdersController(
                GetCartService.create(cartPersistencePort, bookInfoPort),
                AddCartItemService.create(cartPersistencePort, bookInfoPort),
                UpdateCartItemService.create(cartPersistencePort, bookInfoPort),
                RemoveCartItemService.create(cartPersistencePort, bookInfoPort),
                CheckoutService.create(cartPersistencePort, orderPersistencePort, bookInfoPort,
                        bookStockPort, paymentPort, transactionPort),
                GetOrderByIdService.create(orderPersistencePort),
                GetOrderHistoryService.create(orderPersistencePort),
                CancelOrderService.create(orderPersistencePort, bookStockPort, refundPort, transactionPort),
                UpdateOrderStatusService.create(orderPersistencePort),
                OrderPresenter.create()
        );
    }

    public CartViewModel getCart(UUID customerId) {
        return presenter.present(getCartUseCase.execute(customerId));
    }

    public CartViewModel addCartItem(AddCartItemCommand command) {
        return presenter.present(addCartItemUseCase.execute(command));
    }

    public CartViewModel updateCartItem(UpdateCartItemCommand command) {
        return presenter.present(updateCartItemUseCase.execute(command));
    }

    public CartViewModel removeCartItem(UUID customerId, UUID bookId) {
        return presenter.present(removeCartItemUseCase.execute(customerId, bookId));
    }

    public OrderViewModel checkout(UUID customerId, PaymentMethod paymentMethod) {
        return presenter.present(checkoutUseCase.execute(customerId, paymentMethod));
    }

    public OrderViewModel getOrderById(UUID orderId, UUID requesterId, boolean isAdmin) {
        return presenter.present(getOrderByIdUseCase.execute(orderId, requesterId, isAdmin));
    }

    public PageResult<OrderSummaryViewModel> getOrderHistory(UUID customerId, PageQuery pageQuery) {
        return getOrderHistoryUseCase.execute(customerId, pageQuery).map(presenter::presentSummary);
    }

    public OrderViewModel cancelOrder(UUID orderId, UUID customerId) {
        return presenter.present(cancelOrderUseCase.execute(orderId, customerId));
    }

    public OrderViewModel updateOrderStatus(UpdateOrderStatusCommand command) {
        return presenter.present(updateOrderStatusUseCase.execute(command));
    }
}
