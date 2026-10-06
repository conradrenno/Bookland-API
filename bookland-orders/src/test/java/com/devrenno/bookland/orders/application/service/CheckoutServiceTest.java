package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.PaymentOutcome;
import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.dto.StockReservationOutcome;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.PaymentDeclinedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private BookInfoPort bookInfoPort;
    @Mock private StockReservationPort stockReservationPort;
    @Mock private PaymentPort paymentPort;

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

    private CheckoutService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = CheckoutService.create(cartPersistencePort, orderPersistencePort,
                bookInfoPort, stockReservationPort, paymentPort, transactionPort);
    }

    /** The order the saga will keep: reserve first, then charge — never the other way round. */
    @Test
    void execute_shouldReserveThenChargeThenConfirm_whenEverythingSucceeds() {
        givenACartOf(2, 10);
        when(stockReservationPort.reserve(any(), anyList())).thenReturn(new StockReservationOutcome(true, List.of()));
        when(paymentPort.charge(any(), eq(customerId), any(), eq(PaymentMethod.CREDIT_CARD)))
                .thenReturn(new PaymentOutcome(true, null));
        when(orderPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = service.execute(customerId, PaymentMethod.CREDIT_CARD);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getStatusHistory()).extracting("toStatus")
                .containsExactly(OrderStatus.AWAITING_PAYMENT, OrderStatus.CONFIRMED);
        InOrder steps = inOrder(stockReservationPort, paymentPort);
        steps.verify(stockReservationPort).reserve(eq(order.getId()), eq(List.of(new StockLine(bookId, 2))));
        steps.verify(paymentPort).charge(any(), any(), any(), any());
        verify(cartPersistencePort).deleteByCustomerId(customerId);
        verify(stockReservationPort, never()).release(any());
    }

    /**
     * The failure the new order removes: losing the race for the last copies used to surface after
     * the payment had been approved. Now it surfaces at the reservation, and nobody is charged.
     */
    @Test
    void execute_shouldChargeNothing_whenTheReservationFails() {
        givenACartOf(2, 10);
        when(stockReservationPort.reserve(any(), anyList()))
                .thenReturn(new StockReservationOutcome(false, List.of(bookId)));

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOfSatisfying(CartItemUnavailableException.class,
                        e -> assertThat(e.getMessage()).contains(bookId.toString()));

        verifyNoInteractions(paymentPort);
        verify(orderPersistencePort, never()).save(any());
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
    }

    /** The compensation: a declined charge gives the reserved stock back, and the cart stays. */
    @Test
    void execute_shouldReleaseTheReservationAndKeepTheCart_whenPaymentIsDeclined() {
        givenACartOf(2, 10);
        when(stockReservationPort.reserve(any(), anyList())).thenReturn(new StockReservationOutcome(true, List.of()));
        when(paymentPort.charge(any(), any(), any(), any())).thenReturn(new PaymentOutcome(false, "Insufficient funds"));

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(PaymentDeclinedException.class);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderPersistencePort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(saved.getValue().getStatusReason()).isEqualTo("Insufficient funds");
        verify(stockReservationPort).release(saved.getValue().getId());
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
    }

    /** A decline that came without a reason must not be mistaken for an approval. */
    @Test
    void execute_shouldStillReportADecline_whenTheReasonIsMissing() {
        givenACartOf(1, 10);
        when(stockReservationPort.reserve(any(), anyList())).thenReturn(new StockReservationOutcome(true, List.of()));
        when(paymentPort.charge(any(), any(), any(), any())).thenReturn(new PaymentOutcome(false, null));

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(PaymentDeclinedException.class);
        verify(stockReservationPort).release(any());
    }

    @Test
    void execute_shouldThrowCartItemUnavailable_beforeReserving_whenStockIsVisiblyShort() {
        givenACartOf(5, 2);

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class);

        verifyNoInteractions(stockReservationPort, paymentPort);
    }

    @Test
    void execute_shouldThrowCartItemUnavailable_whenBookWasRemovedFromCatalog() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(buildCart(1)));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class);

        verifyNoInteractions(stockReservationPort, paymentPort);
    }

    @Test
    void execute_shouldThrowCartNotFound_whenCartDoesNotExist() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartNotFoundException.class);
    }

    private void givenACartOf(int quantity, int stock) {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(buildCart(quantity)));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(
                new BookInfo(bookId, "Clean Code", "/media/covers/clean-code.jpg", BigDecimal.valueOf(29.90), stock)));
    }

    private Cart buildCart(int quantity) {
        return Cart.reconstitute(
                UUID.randomUUID(), customerId,
                List.of(CartItem.of(bookId, quantity, BigDecimal.valueOf(29.90))),
                Instant.now(), Instant.now()
        );
    }
}
