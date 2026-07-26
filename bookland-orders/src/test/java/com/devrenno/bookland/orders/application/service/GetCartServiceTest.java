package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCartServiceTest {

    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private BookInfoPort bookInfoPort;

    private GetCartService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = GetCartService.create(cartPersistencePort, bookInfoPort);
    }

    private Cart buildCart(int quantity) {
        Cart cart = Cart.createFor(customerId);
        cart.addOrUpdateItem(bookId, BigDecimal.valueOf(29.90), quantity, 100);
        return cart;
    }

    @Test
    void execute_shouldEnrichItemsWithTitleAndCover() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(buildCart(2)));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(new BookInfo(
                bookId, "Clean Code", "/media/covers/clean-code.jpg", BigDecimal.valueOf(34.90), 10)));

        CartView view = service.execute(customerId);

        assertThat(view.items()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("Clean Code");
            assertThat(item.coverImageUrl()).isEqualTo("/media/covers/clean-code.jpg");
            assertThat(item.available()).isTrue();
            // price stays the one snapshotted at addition, not the catalog's current price
            assertThat(item.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(29.90));
        });
    }

    @Test
    void execute_shouldMarkItemUnavailable_whenStockIsBelowTheCartQuantity() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(buildCart(5)));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.of(new BookInfo(
                bookId, "Clean Code", null, BigDecimal.valueOf(29.90), 2)));

        assertThat(service.execute(customerId).items())
                .singleElement()
                .satisfies(item -> assertThat(item.available()).isFalse());
    }

    @Test
    void execute_shouldDegradeGracefully_whenBookIsNoLongerInTheCatalog() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(buildCart(1)));
        when(bookInfoPort.findBookInfo(bookId)).thenReturn(Optional.empty());

        assertThat(service.execute(customerId).items()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("Unavailable");
            assertThat(item.coverImageUrl()).isNull();
            assertThat(item.available()).isFalse();
        });
    }

    @Test
    void execute_shouldReturnEmptyCart_whenCustomerHasNone() {
        when(cartPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.empty());

        CartView view = service.execute(customerId);

        assertThat(view.customerId()).isEqualTo(customerId);
        assertThat(view.items()).isEmpty();
    }
}
