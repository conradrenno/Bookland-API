package com.devrenno.bookland.orders.domain.entity;

import com.devrenno.bookland.orders.domain.exception.InvalidOrderStatusTransitionException;
import com.devrenno.bookland.orders.domain.exception.OrderAccessDeniedException;
import com.devrenno.bookland.orders.domain.exception.OrderCancellationNotAllowedException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
public class Order {

    /**
     * An order cannot be cancelled while its checkout is still running ({@code PENDING},
     * {@code AWAITING_PAYMENT}): a cancellation racing the saga's next step — a payment approved a
     * moment after the order was cancelled — would leave stock or money behind.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING,          Set.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.REJECTED),
            OrderStatus.AWAITING_PAYMENT, Set.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED),
            OrderStatus.CONFIRMED,        Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,          Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,        Set.of(),
            OrderStatus.CANCELLED,        Set.of(),
            OrderStatus.PAYMENT_FAILED,   Set.of(),
            OrderStatus.REJECTED,         Set.of()
    );

    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    /** Why the checkout did not complete (REJECTED, PAYMENT_FAILED); null otherwise. */
    private String statusReason;
    private final BigDecimal totalAmount;
    private final List<StatusTransition> statusHistory;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(UUID id, UUID customerId, List<OrderItem> items, OrderStatus status, String statusReason,
                  BigDecimal totalAmount, List<StatusTransition> statusHistory,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.status = status;
        this.statusReason = statusReason;
        this.totalAmount = totalAmount;
        this.statusHistory = statusHistory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order fromCart(UUID customerId, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = Instant.now();
        return new Order(UUID.randomUUID(), customerId, new ArrayList<>(items),
                OrderStatus.PENDING, null, total, new ArrayList<>(), now, now);
    }

    public static Order reconstitute(UUID id, UUID customerId, List<OrderItem> items, OrderStatus status,
                                     String statusReason, BigDecimal totalAmount,
                                     List<StatusTransition> statusHistory, Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, new ArrayList<>(items), status, statusReason, totalAmount,
                new ArrayList<>(statusHistory), createdAt, updatedAt);
    }

    public void cancel(UUID requesterId) {
        if (!customerId.equals(requesterId)) throw new OrderAccessDeniedException(id);
        if (status != OrderStatus.CONFIRMED) {
            throw new OrderCancellationNotAllowedException(id, status);
        }
        transitionStatus(OrderStatus.CANCELLED, requesterId);
    }

    /** A transition that ends the checkout unsuccessfully, recording why. */
    public void transitionStatus(OrderStatus newStatus, UUID changedBy, String reason) {
        transitionStatus(newStatus, changedBy);
        this.statusReason = reason;
    }

    public void transitionStatus(OrderStatus newStatus, UUID changedBy) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(this.id, this.status, newStatus);
        }
        StatusTransition transition = StatusTransition.create(this.id, this.status, newStatus, changedBy);
        statusHistory.add(transition);
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }
}
