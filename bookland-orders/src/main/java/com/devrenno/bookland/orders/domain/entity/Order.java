package com.devrenno.bookland.orders.domain.entity;

import com.devrenno.bookland.orders.domain.exception.InvalidOrderStatusTransitionException;
import com.devrenno.bookland.orders.domain.exception.OrderAccessDeniedException;
import com.devrenno.bookland.orders.domain.exception.OrderCancellationNotAllowedException;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class Order {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.AWAITING_PAYMENT, Set.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,        Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,          Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,        Set.of(),
            OrderStatus.CANCELLED,        Set.of(),
            OrderStatus.PAYMENT_FAILED,   Set.of()
    );

    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final BigDecimal totalAmount;
    @Builder.Default
    private List<StatusTransition> statusHistory = new ArrayList<>();
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Order fromCart(UUID customerId, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .items(new ArrayList<>(items))
                .status(OrderStatus.AWAITING_PAYMENT)
                .totalAmount(total)
                .statusHistory(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void cancel(UUID requesterId) {
        if (!customerId.equals(requesterId)) throw new OrderAccessDeniedException(id);
        if (status != OrderStatus.AWAITING_PAYMENT && status != OrderStatus.CONFIRMED) {
            throw new OrderCancellationNotAllowedException(id, status);
        }
        transitionStatus(OrderStatus.CANCELLED, requesterId);
    }

    public void transitionStatus(OrderStatus newStatus, UUID changedBy) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(this.id, this.status, newStatus);
        }
        StatusTransition transition = StatusTransition.create(this.id, this.status, newStatus, changedBy);
        statusHistory.add(transition);
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}
