package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderAccessDeniedException;
import com.devrenno.bookland.orders.domain.exception.OrderCancellationNotAllowedException;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private BookStockPort bookStockPort;
    @Mock private RefundPort refundPort;
    @InjectMocks private CancelOrderService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @Test
    void execute_shouldCancelWithoutRefund_whenOrderIsAwaitingPayment() {
        Order order = buildOrder(customerId, OrderStatus.AWAITING_PAYMENT);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        OrderResponse response = service.execute(order.getId(), customerId);

        assertThat(response).isNotNull();
        verify(bookStockPort, never()).adjustStock(any(), anyInt());
        verify(refundPort, never()).refund(any());
    }

    @Test
    void execute_shouldRestoreStockAndRefund_whenOrderIsConfirmed() {
        Order order = buildOrder(customerId, OrderStatus.CONFIRMED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        OrderResponse response = service.execute(order.getId(), customerId);

        assertThat(response).isNotNull();
        verify(bookStockPort).adjustStock(bookId, 2);
        verify(refundPort).refund(order.getId());
    }

    @Test
    void execute_shouldThrowOrderNotFound_whenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(orderId, customerId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void execute_shouldThrowAccessDenied_whenRequesterIsNotOwner() {
        UUID otherCustomer = UUID.randomUUID();
        Order order = buildOrder(customerId, OrderStatus.AWAITING_PAYMENT);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(order.getId(), otherCustomer))
                .isInstanceOf(OrderAccessDeniedException.class);

        verify(bookStockPort, never()).adjustStock(any(), anyInt());
    }

    @Test
    void execute_shouldThrowCancellationNotAllowed_whenOrderIsShipped() {
        Order order = buildOrder(customerId, OrderStatus.SHIPPED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(order.getId(), customerId))
                .isInstanceOf(OrderCancellationNotAllowedException.class);

        verify(bookStockPort, never()).adjustStock(any(), anyInt());
    }

    private Order buildOrder(UUID customerId, OrderStatus status) {
        OrderItem item = OrderItem.builder()
                .bookId(bookId)
                .title("Clean Code")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(29.90))
                .build();
        return Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .status(status)
                .totalAmount(BigDecimal.valueOf(59.80))
                .items(new ArrayList<>(List.of(item)))
                .statusHistory(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
