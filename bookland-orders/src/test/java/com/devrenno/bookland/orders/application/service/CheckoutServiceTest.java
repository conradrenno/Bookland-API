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
import java.time.LocalDateTime;
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

        Order order = service.execute(customerId, PaymentMethod.CREDIT_CARD);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        verify(bookStockPort).adjustStock(bookId, -2);
        verify(cartPersistencePort).deleteByCustomerId(customerId);
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
        verify(bookStockPort, never()).adjustStock(any(), anyInt());
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
        verify(bookStockPort, never()).adjustStock(any(), anyInt());
    }

    @Test
    void execute_shouldThrowCartItemUnavailable_whenBookWasRemovedFromCatalog() {
        Cart cart = buildCart(bookId, 1, BigDecimal.valueOf(29.90));

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(CartItemUnavailableException.class);

        verify(orderPersistencePort, never()).save(any());
        verify(bookStockPort, never()).adjustStock(any(), anyInt());
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
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
