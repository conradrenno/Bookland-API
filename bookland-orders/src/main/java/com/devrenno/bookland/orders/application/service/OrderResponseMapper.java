package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.CartItemResponse;
import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.dto.OrderItemResponse;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.dto.OrderSummaryResponse;
import com.devrenno.bookland.orders.application.dto.StatusTransitionResponse;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.Order;

import java.math.BigDecimal;
import java.util.List;

final class OrderResponseMapper {

    private OrderResponseMapper() {}

    static CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartItemResponse(
                        i.getBookId(), i.getQuantity(), i.getUnitPriceAtAddition(),
                        i.getUnitPriceAtAddition().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), cart.getCustomerId(), items, total, cart.getUpdatedAt());
    }

    static OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getBookId(), i.getTitle(), i.getQuantity(), i.getUnitPrice(), i.subtotal()
                ))
                .toList();
        List<StatusTransitionResponse> history = order.getStatusHistory().stream()
                .map(t -> new StatusTransitionResponse(
                        t.getFromStatus(), t.getToStatus(), t.getChangedAt(), t.getChangedBy()
                ))
                .toList();
        return new OrderResponse(
                order.getId(), order.getCustomerId(), items, order.getStatus(),
                order.getTotalAmount(), history, order.getCreatedAt(), order.getUpdatedAt()
        );
    }

    static OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(), order.getStatus(), order.getTotalAmount(),
                order.getItems().size(), order.getCreatedAt()
        );
    }
}
