package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.PaymentDeclinedException;
import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
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
class CheckoutServiceTest {

    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private BookInfoPort bookInfoPort;
    @Mock private BookStockPort bookStockPort;
    @Mock private PaymentPort paymentPort;
    @InjectMocks private CheckoutService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @Test
    void execute_shouldCreateOrderClearCartAndDecrementStock_whenPaymentApproved() {
        Cart cart = buildCart(bookId, 2, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", BigDecimal.valueOf(29.90), 10);
        Order awaitingOrder = Order.fromCart(customerId, List.of());

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.getBookInfo(bookId)).thenReturn(book);
        when(orderPersistencePort.save(any())).thenReturn(awaitingOrder);
        when(paymentPort.processPayment(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(true, "SIM-001", null));

        OrderResponse response = service.execute(customerId, PaymentMethod.CREDIT_CARD);

        assertThat(response).isNotNull();
        verify(bookStockPort).adjustStock(bookId, -2);
        verify(cartPersistencePort).deleteByCustomerId(customerId);
    }

    @Test
    void execute_shouldSaveOrderAsPaymentFailed_whenPaymentDeclined() {
        Cart cart = buildCart(bookId, 2, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", BigDecimal.valueOf(29.90), 10);
        Order awaitingOrder = Order.fromCart(customerId, List.of());

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.getBookInfo(bookId)).thenReturn(book);
        when(orderPersistencePort.save(any())).thenReturn(awaitingOrder);
        when(paymentPort.processPayment(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(false, null, "insufficient funds"));

        assertThatThrownBy(() -> service.execute(customerId, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(PaymentDeclinedException.class);

        verify(orderPersistencePort).save(any());
        verify(bookStockPort, never()).adjustStock(any(), anyInt());
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
    }

    @Test
    void execute_shouldThrowCartItemUnavailable_whenStockInsufficient() {
        Cart cart = buildCart(bookId, 5, BigDecimal.valueOf(29.90));
        BookInfo book = new BookInfo(bookId, "Clean Code", BigDecimal.valueOf(29.90), 2);

        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(bookInfoPort.getBookInfo(bookId)).thenReturn(book);

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
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(bookId, quantity, price));
        return Cart.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .items(items)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
