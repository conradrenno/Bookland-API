package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderAccessDeniedException;
import com.devrenno.bookland.orders.domain.exception.OrderCancellationNotAllowedException;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private StockReservationPort stockReservationPort;
    @Mock private RefundPort refundPort;

    /** Pass-through fake: runs the unit of work inline, no transaction machinery in unit tests. */
    private final TransactionPort transactionPort = new TransactionPort() {
        @Override
        public void inTransaction(Runnable work) {
            work.run();
        }

        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    private CancelOrderService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = CancelOrderService.create(orderPersistencePort, stockReservationPort, refundPort, transactionPort);
    }

    /**
     * While the checkout saga is still running, cancelling would race its next step — a payment
     * approved a moment after the order was cancelled leaves money or stock behind.
     */
    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "AWAITING_PAYMENT"})
    void execute_shouldRefuse_whileTheCheckoutIsStillRunning(OrderStatus status) {
        Order order = buildOrder(customerId, status);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(order.getId(), customerId))
                .isInstanceOf(OrderCancellationNotAllowedException.class);
        verify(stockReservationPort, never()).release(any());
        verify(refundPort, never()).refund(any());
    }

    @Test
    void execute_shouldRestoreStockAndRefund_whenOrderIsConfirmed() {
        Order order = buildOrder(customerId, OrderStatus.CONFIRMED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        Order result = service.execute(order.getId(), customerId);

        assertThat(result).isNotNull();
        verify(stockReservationPort).release(order.getId());
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
        Order order = buildOrder(customerId, OrderStatus.CONFIRMED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(order.getId(), otherCustomer))
                .isInstanceOf(OrderAccessDeniedException.class);

        verify(stockReservationPort, never()).release(any());
    }

    @Test
    void execute_shouldThrowCancellationNotAllowed_whenOrderIsShipped() {
        Order order = buildOrder(customerId, OrderStatus.SHIPPED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(order.getId(), customerId))
                .isInstanceOf(OrderCancellationNotAllowedException.class);

        verify(stockReservationPort, never()).release(any());
    }

    private Order buildOrder(UUID customerId, OrderStatus status) {
        OrderItem item = OrderItem.of(bookId, "Clean Code", "/media/covers/clean-code.jpg", 2, BigDecimal.valueOf(29.90));
        return Order.reconstitute(
                UUID.randomUUID(), customerId, List.of(item), status, null, null,
                BigDecimal.valueOf(59.80), List.of(),
                Instant.now(), Instant.now()
        );
    }
}
