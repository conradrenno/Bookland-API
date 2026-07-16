package com.devrenno.bookland.orders.adapters.presenter;

import com.devrenno.bookland.orders.adapters.viewmodel.CartItemViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.CartViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderItemViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.StatusTransitionViewModel;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Plain-Java presenter: shapes orders domain entities into the HTTP-facing view models.
 */
public class OrderPresenter {

    private OrderPresenter() {}

    public static OrderPresenter create() {
        return new OrderPresenter();
    }

    public CartViewModel present(Cart cart) {
        List<CartItemViewModel> items = cart.getItems().stream()
                .map(i -> new CartItemViewModel(
                        i.getBookId(), i.getQuantity(), i.getUnitPriceAtAddition(),
                        i.getUnitPriceAtAddition().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemViewModel::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartViewModel(cart.getId(), cart.getCustomerId(), items, total, cart.getUpdatedAt());
    }

    public OrderViewModel present(Order order) {
        List<OrderItemViewModel> items = order.getItems().stream()
                .map(i -> new OrderItemViewModel(
                        i.getBookId(), i.getTitle(), i.getQuantity(), i.getUnitPrice(), i.subtotal()
                ))
                .toList();
        List<StatusTransitionViewModel> history = order.getStatusHistory().stream()
                .map(t -> new StatusTransitionViewModel(
                        t.getFromStatus(), t.getToStatus(), t.getChangedAt(), t.getChangedBy()
                ))
                .toList();
        return new OrderViewModel(
                order.getId(), order.getCustomerId(), items, order.getStatus(),
                order.getTotalAmount(), history, order.getCreatedAt(), order.getUpdatedAt()
        );
    }

    public OrderSummaryViewModel presentSummary(Order order) {
        return new OrderSummaryViewModel(
                order.getId(), order.getStatus(), order.getTotalAmount(),
                order.getItems().size(), order.getCreatedAt()
        );
    }
}
