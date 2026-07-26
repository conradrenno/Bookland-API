package com.devrenno.bookland.orders.adapters.presenter;

import com.devrenno.bookland.orders.adapters.viewmodel.AdminOrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.CartItemViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.CartViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderItemViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.StatusTransitionViewModel;
import com.devrenno.bookland.orders.application.dto.CartView;
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

    public CartViewModel present(CartView cart) {
        List<CartItemViewModel> items = cart.items().stream()
                .map(i -> new CartItemViewModel(
                        i.bookId(), i.title(), i.coverImageUrl(), i.quantity(), i.unitPrice(),
                        i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())), i.available()
                ))
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemViewModel::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartViewModel(cart.id(), cart.customerId(), items, total, cart.updatedAt());
    }

    public OrderViewModel present(Order order) {
        List<OrderItemViewModel> items = order.getItems().stream()
                .map(i -> new OrderItemViewModel(
                        i.getBookId(), i.getTitle(), i.getCoverImageUrl(),
                        i.getQuantity(), i.getUnitPrice(), i.subtotal()
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

    public AdminOrderSummaryViewModel presentAdminSummary(Order order) {
        return new AdminOrderSummaryViewModel(
                order.getId(), order.getCustomerId(), order.getStatus(), order.getTotalAmount(),
                order.getItems().size(), order.getCreatedAt()
        );
    }
}
