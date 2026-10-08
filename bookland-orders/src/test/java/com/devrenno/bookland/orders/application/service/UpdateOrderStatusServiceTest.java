package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.InvalidOrderStatusTransitionException;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusServiceTest {

    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private OrderEventPort orderEventPort;

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

    private UpdateOrderStatusService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = UpdateOrderStatusService.create(orderPersistencePort, orderEventPort, transactionPort);
    }

    /**
     * The back-office cancellation owes exactly what the customer's does. Before this was wired, the
     * admin path only moved the status: the customer stayed charged and the copies never returned to
     * the catalog.
     */
    @Test
    void execute_shouldAnnounceTheCancellation_whenAdminCancelsConfirmedOrder() {
        Order order = buildOrder(OrderStatus.CONFIRMED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        Order result = service.execute(
                new UpdateOrderStatusCommand(order.getId(), OrderStatus.CANCELLED, adminId));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPort).orderCancelled(order);
    }

    /**
     * Shipping is not a cancellation: CONFIRMED as the previous status must not be enough to
     * compensate. It is announced as what it is, for the customer to be told.
     */
    @Test
    void execute_shouldAnnounceTheShipmentAndNotCompensate_whenAdminShipsConfirmedOrder() {
        Order order = buildOrder(OrderStatus.CONFIRMED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        Order result = service.execute(
                new UpdateOrderStatusCommand(order.getId(), OrderStatus.SHIPPED, adminId));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderEventPort).orderShipped(order);
        verify(orderEventPort, never()).orderCancelled(any());
    }

    /** Nobody is told about a delivery: the shipment was the customer's last notice. */
    @Test
    void execute_shouldAnnounceNothing_whenAdminMarksTheOrderDelivered() {
        Order order = buildOrder(OrderStatus.SHIPPED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        service.execute(new UpdateOrderStatusCommand(order.getId(), OrderStatus.DELIVERED, adminId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verifyNoInteractions(orderEventPort);
    }

    /** The back office cannot cancel mid-checkout either: the transition does not exist. */
    @Test
    void execute_shouldRefuseToCancel_whileTheCheckoutIsStillRunning() {
        Order order = buildOrder(OrderStatus.AWAITING_PAYMENT);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(
                new UpdateOrderStatusCommand(order.getId(), OrderStatus.CANCELLED, adminId)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        verifyNoInteractions(orderEventPort);
    }

    @Test
    void execute_shouldThrowAndCompensateNothing_whenTransitionIsIllegal() {
        Order order = buildOrder(OrderStatus.DELIVERED);

        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(
                new UpdateOrderStatusCommand(order.getId(), OrderStatus.CANCELLED, adminId)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        verifyNoInteractions(orderEventPort);
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldThrowOrderNotFound_whenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new UpdateOrderStatusCommand(orderId, OrderStatus.SHIPPED, adminId)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private Order buildOrder(OrderStatus status) {
        OrderItem item = OrderItem.of(bookId, "Clean Code", "/media/covers/clean-code.jpg", 2, BigDecimal.valueOf(29.90));
        return Order.reconstitute(
                UUID.randomUUID(), customerId, "reader@bookland.com", "Reader", List.of(item), status, null, null,
                BigDecimal.valueOf(59.80), List.of(),
                Instant.now(), Instant.now()
        );
    }
}
