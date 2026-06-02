package com.devrenno.bookland.orders.infrastructure.persistence.adapter;

import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.entity.StatusTransition;
import com.devrenno.bookland.orders.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.devrenno.bookland.orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.devrenno.bookland.orders.infrastructure.persistence.entity.StatusTransitionJpaEntity;
import com.devrenno.bookland.orders.infrastructure.persistence.repository.OrderJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderPersistencePort {

    private final OrderJpaRepository orderRepository;
    private final EntityManager entityManager;

    @Override
    public Order save(Order order) {
        Optional<OrderJpaEntity> existing = orderRepository.findById(order.getId());

        if (existing.isEmpty()) {
            OrderJpaEntity entity = buildEntity(order);
            entityManager.persist(entity);
            entityManager.flush();
            return toDomain(entity);
        }

        OrderJpaEntity entity = existing.get();
        entity.setStatus(order.getStatus().name());
        entity.setUpdatedAt(order.getUpdatedAt());

        // Items are immutable after order creation — never touch them on update.
        // Status transitions are append-only — add only entries not already persisted.
        Set<UUID> existingTransitionIds = entity.getStatusHistory().stream()
                .map(StatusTransitionJpaEntity::getId)
                .collect(Collectors.toSet());

        order.getStatusHistory().stream()
                .filter(t -> !existingTransitionIds.contains(t.getId()))
                .map(t -> StatusTransitionJpaEntity.builder()
                        .id(t.getId())
                        .order(entity)
                        .fromStatus(t.getFromStatus().name())
                        .toStatus(t.getToStatus().name())
                        .changedAt(t.getChangedAt())
                        .changedBy(t.getChangedBy())
                        .build())
                .forEach(entity.getStatusHistory()::add);

        return toDomain(orderRepository.save(entity));
    }

    private OrderJpaEntity buildEntity(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        List<OrderItemJpaEntity> itemEntities = order.getItems().stream()
                .map(i -> OrderItemJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .order(entity)
                        .bookId(i.getBookId())
                        .title(i.getTitle())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();
        entity.getItems().addAll(itemEntities);

        List<StatusTransitionJpaEntity> historyEntities = order.getStatusHistory().stream()
                .map(t -> StatusTransitionJpaEntity.builder()
                        .id(t.getId())
                        .order(entity)
                        .fromStatus(t.getFromStatus().name())
                        .toStatus(t.getToStatus().name())
                        .changedAt(t.getChangedAt())
                        .changedBy(t.getChangedBy())
                        .build())
                .toList();
        entity.getStatusHistory().addAll(historyEntities);

        return entity;
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderRepository.findById(orderId).map(this::toDomain);
    }

    @Override
    public Page<Order> findByCustomerId(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toDomain);
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> OrderItem.builder()
                        .bookId(i.getBookId())
                        .title(i.getTitle())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        List<StatusTransition> history = entity.getStatusHistory().stream()
                .map(t -> StatusTransition.builder()
                        .id(t.getId())
                        .orderId(entity.getId())
                        .fromStatus(OrderStatus.valueOf(t.getFromStatus()))
                        .toStatus(OrderStatus.valueOf(t.getToStatus()))
                        .changedAt(t.getChangedAt())
                        .changedBy(t.getChangedBy())
                        .build())
                .toList();

        return Order.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .status(OrderStatus.valueOf(entity.getStatus()))
                .totalAmount(entity.getTotalAmount())
                .items(new ArrayList<>(items))
                .statusHistory(new ArrayList<>(history))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
