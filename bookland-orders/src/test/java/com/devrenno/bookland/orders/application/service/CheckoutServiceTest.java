package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CheckoutCommand;
import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.CheckoutCommandPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.CheckoutInProgressException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private BookInfoPort bookInfoPort;
    @Mock private CheckoutCommandPort checkoutCommandPort;

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
        service = CheckoutService.create(cartPersistencePort, orderPersistencePort, bookInfoPort,
                checkoutCommandPort, transactionPort);
    }

    /** The checkout only starts the saga: a PENDING order and the first command, nothing charged yet. */
    @Test
    void execute_shouldClaimTheCartSaveAPendingOrderAndRequestTheReservation() {
        givenACartOf(2, 10);
        when(cartPersistencePort.claimForCheckout(eq(customerId), any())).thenReturn(true);
        when(orderPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = service.execute(checkout());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
        assertThat(order.getCustomerEmail()).isEqualTo("reader@bookland.com");
        assertThat(order.getCustomerName()).isEqualTo("Reader");
        verify(cartPersistencePort).claimForCheckout(customerId, order.getId());
        verify(checkoutCommandPort).requestStockReservation(order.getId(), List.of(new StockLine(bookId, 2)));
        verify(checkoutCommandPort, never()).requestPayment(any(), any(), any(), any());
        verify(cartPersistencePort, never()).deleteByCustomerId(any());
    }

    /** Two sagas for one cart would both charge the customer. */
    @Test
    void execute_shouldRefuse_whenAnotherCheckoutHoldsTheCart() {
        givenACartOf(2, 10);
        when(cartPersistencePort.claimForCheckout(eq(customerId), any())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(checkout()))
                .isInstanceOf(CheckoutInProgressException.class);

        verify(orderPersistencePort, never()).save(any());
        verifyNoInteractions(checkoutCommandPort);
    }

    @Test
    void execute_shouldFailAtOnce_whenStockIsVisiblyShort() {
        givenACartOf(5, 2);

        assertThatThrownBy(() -> service.execute(checkout()))
                .isInstanceOf(CartItemUnavailableException.class);

        verify(cartPersistencePort, never()).claimForCheckout(any(), any());
        verifyNoInteractions(checkoutCommandPort);
    }

    /** Unknown is not "available": the order would snapshot titles it never read. */
    @Test
    void execute_shouldRefuse_whenTheCatalogCannotBeReached() {
        givenACartOf(1, 10);
        when(bookInfoPort.findBookInfos(List.of(bookId))).thenThrow(new CatalogUnavailableException("down", null));

        assertThatThrownBy(() -> service.execute(checkout()))
                .isInstanceOf(CatalogUnavailableException.class);

        verify(cartPersistencePort, never()).claimForCheckout(any(), any());
        verifyNoInteractions(checkoutCommandPort);
    }

    @Test
    void execute_shouldThrowCartNotFound_whenCartDoesNotExist() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(checkout()))
                .isInstanceOf(CartNotFoundException.class);
    }

    private void givenACartOf(int quantity, int stock) {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(Cart.reconstitute(
                UUID.randomUUID(), customerId, List.of(CartItem.of(bookId, quantity, BigDecimal.valueOf(29.90))),
                Instant.now(), Instant.now())));
        when(bookInfoPort.findBookInfos(List.of(bookId))).thenReturn(Map.of(bookId,
                new BookInfo(bookId, "Clean Code", null, BigDecimal.valueOf(29.90), stock)));
    }

    private CheckoutCommand checkout() {
        return new CheckoutCommand(customerId, "reader@bookland.com", "Reader", PaymentMethod.PIX);
    }
}
