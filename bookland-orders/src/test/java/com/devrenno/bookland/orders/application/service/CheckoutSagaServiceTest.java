package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.CheckoutCommandPort;
import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutSagaServiceTest {

    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private CheckoutCommandPort checkoutCommandPort;
    @Mock private OrderEventPort orderEventPort;

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

    private CheckoutSagaService saga;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        saga = CheckoutSagaService.create(orderPersistencePort, cartPersistencePort, checkoutCommandPort,
                orderEventPort, transactionPort);
    }

    @Test
    void stockReserved_shouldMoveToAwaitingPaymentAndRequestTheCharge() {
        Order order = orderIn(OrderStatus.PENDING);

        assertThat(saga.onStockReserved(order.getId())).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(checkoutCommandPort).requestPayment(order.getId(), customerId, order.getTotalAmount(), PaymentMethod.PIX);
        verifyNoInteractions(orderEventPort);
    }

    @Test
    void stockReservationFailed_shouldRejectWithTheBooksAndFreeTheCart() {
        Order order = orderIn(OrderStatus.PENDING);

        saga.onStockReservationFailed(order.getId(), List.of(bookId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getStatusReason()).contains(bookId.toString());
        verify(cartPersistencePort).releaseCheckoutClaim(customerId);
        verifyNoInteractions(checkoutCommandPort);
        verify(orderEventPort).orderRejected(order);
    }

    @Test
    void paymentApproved_shouldConfirmAndEmptyTheCart() {
        Order order = orderIn(OrderStatus.AWAITING_PAYMENT);

        saga.onPaymentApproved(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartPersistencePort).deleteByCustomerId(customerId);
        verifyNoInteractions(checkoutCommandPort);
        verify(orderEventPort).orderConfirmed(order);
    }

    /** The compensation: the declined order asks the catalog for the reserved stock back. */
    @Test
    void paymentDeclined_shouldFailTheOrderReleaseTheStockAndFreeTheCart() {
        Order order = orderIn(OrderStatus.AWAITING_PAYMENT);

        saga.onPaymentDeclined(order.getId(), "Amount above the simulated card limit");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getStatusReason()).isEqualTo("Amount above the simulated card limit");
        verify(checkoutCommandPort).requestStockRelease(order.getId());
        verify(cartPersistencePort).releaseCheckoutClaim(customerId);
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
        verify(orderEventPort).orderPaymentFailed(order);
    }

    /**
     * Idempotency by state: a StockReserved arriving again after the order moved on — a duplicate the
     * inbox did not catch, such as a second reply to a second command — must not ask for a second charge.
     */
    @Test
    void aReplyForAStepAlreadyPast_isIgnored() {
        Order order = orderIn(OrderStatus.AWAITING_PAYMENT);

        assertThat(saga.onStockReserved(order.getId())).isFalse();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verifyNoInteractions(checkoutCommandPort, cartPersistencePort, orderEventPort);
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void aReplyForAnUnknownOrder_isIgnored() {
        UUID orderId = UUID.randomUUID();
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.empty());

        assertThat(saga.onPaymentApproved(orderId)).isFalse();
        verifyNoInteractions(checkoutCommandPort, cartPersistencePort, orderEventPort);
    }

    private Order orderIn(OrderStatus status) {
        Order order = Order.reconstitute(UUID.randomUUID(), customerId, "reader@bookland.com", "Reader",
                List.of(OrderItem.of(bookId, "Clean Code", null, 2, BigDecimal.valueOf(29.90))),
                status, null, PaymentMethod.PIX, BigDecimal.valueOf(59.80), List.of(), Instant.now(), Instant.now());
        when(orderPersistencePort.findById(order.getId())).thenReturn(Optional.of(order));
        return order;
    }
}
