package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.PaymentDeclinedException;
import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private BookInfoPort bookInfoPort;
    @Mock private BookStockPort bookStockPort;
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
                bookInfoPort, bookStockPort, paymentPort, transactionPort);
    }

    @Test
    void execute_shouldCreateOrderClearCartAndDecrementStock_whenPaymentApproved() {
        Cart cart = buildCart(bookId, 2, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", "/media/covers/clean-code.jpg", BigDecimal.valueOf(29.90), 10);

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(book));
        when(orderPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentPort.processPayment(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(true, "SIM-001", null));
        when(bookStockPort.tryDecrementStock(bookId, 2)).thenReturn(true);

        Order order = service.execute(customerId, PaymentMethod.CREDIT_CARD);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        verify(bookStockPort).tryDecrementStock(bookId, 2);
        verify(cartPersistencePort).deleteByCustomerId(customerId);
    }

    /**
     * The race the conditional decrement exists for: the cart validated against a stock reading that
     * a concurrent checkout consumed before this one reached the decrement. The order must not be
     * confirmed, and the cart must survive so the customer can act on it.
     */
    @Test
    void execute_shouldThrowCartItemUnavailable_whenStockIsTakenBetweenValidationAndDecrement() {
        Cart cart = buildCart(bookId, 1, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", "/media/covers/clean-code.jpg", BigDecimal.valueOf(29.90), 1);

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(book));
        when(paymentPort.processPayment(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(true, "SIM-001", null));
        when(bookStockPort.tryDecrementStock(bookId, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class);

        verify(orderPersistencePort, never()).save(any());
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
    }

    /**
     * A multi-line cart where only the second line lost the race still fails as a whole: the
     * transaction rollback is what undoes the first decrement, so the service must not try to
     * compensate it by hand.
     */
    @Test
    void execute_shouldReportOnlyTheLostLine_whenPartOfTheCartIsStillAvailable() {
        UUID otherBookId = UUID.randomUUID();
        Cart cart = Cart.reconstitute(
                UUID.randomUUID(), customerId,
                List.of(CartItem.of(bookId, 1, BigDecimal.valueOf(29.90)),
                        CartItem.of(otherBookId, 1, BigDecimal.valueOf(49.90))),
                Instant.now(), Instant.now());

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(
                new BookInfo(bookId, "Clean Code", null, BigDecimal.valueOf(29.90), 5)));
        when(bookInfoPort.findBookInfo(otherBookId)).thenReturn(Optional.of(
                new BookInfo(otherBookId, "Refactoring", null, BigDecimal.valueOf(49.90), 1)));
        when(paymentPort.processPayment(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(true, "SIM-001", null));
        when(bookStockPort.tryDecrementStock(bookId, 1)).thenReturn(true);
        when(bookStockPort.tryDecrementStock(otherBookId, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class)
                .hasMessageContaining(otherBookId.toString())
                .hasMessageNotContaining(bookId.toString());

        verify(orderPersistencePort, never()).save(any());
        verify(bookStockPort, never()).incrementStock(any(), anyInt());
    }

    @Test
    void execute_shouldSaveOrderAsPaymentFailed_whenPaymentDeclined() {
        Cart cart = buildCart(bookId, 2, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", "/media/covers/clean-code.jpg", BigDecimal.valueOf(29.90), 10);

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(book));
        when(orderPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentPort.processPayment(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(false, null, "Insufficient funds"));

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(PaymentDeclinedException.class);

        verify(orderPersistencePort).save(any());
        verify(bookStockPort, never()).tryDecrementStock(any(), anyInt());
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
    }

    @Test
    void execute_shouldThrowCartItemUnavailable_whenStockInsufficient() {
        Cart cart = buildCart(bookId, 5, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", "/media/covers/clean-code.jpg", BigDecimal.valueOf(29.90), 2);

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class);

        verify(orderPersistencePort, never()).save(any());
        verify(bookStockPort, never()).tryDecrementStock(any(), anyInt());
    }

    @Test
    void execute_shouldThrowCartItemUnavailable_whenBookWasRemovedFromCatalog() {
        Cart cart = buildCart(bookId, 1, BigDecimal.valueOf(29.90));

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class);

        verify(orderPersistencePort, never()).save(any());
        verify(bookStockPort, never()).tryDecrementStock(any(), anyInt());
    }

    @Test
    void execute_shouldThrowCartNotFound_whenCartDoesNotExist() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartNotFoundException.class);
    }

    private Cart buildCart(UUID bookId, int quantity, BigDecimal price) {
        return Cart.reconstitute(
                UUID.randomUUID(), customerId,
                List.of(CartItem.of(bookId, quantity, price)),
                Instant.now(), Instant.now()
        );
    }
}
