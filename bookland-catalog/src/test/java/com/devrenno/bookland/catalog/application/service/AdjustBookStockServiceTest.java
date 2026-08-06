package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.catalog.domain.valueobject.BookId;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjustBookStockServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;

    private AdjustBookStockService service;

    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = AdjustBookStockService.create(bookPersistencePort);
    }

    @Test
    void adjustStock_shouldIncrement_whenDeltaIsPositive() {
        when(bookPersistencePort.incrementStock(bookId, 10)).thenReturn(true);
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(bookWithStock(15)));

        assertThat(service.adjustStock(bookId, 10)).isEqualTo(15);

        verify(bookPersistencePort).incrementStock(bookId, 10);
        verify(bookPersistencePort, never()).tryDecrementStock(any(), anyInt());
    }

    @Test
    void adjustStock_shouldDecrement_whenDeltaIsNegative() {
        when(bookPersistencePort.tryDecrementStock(bookId, 3)).thenReturn(true);
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(bookWithStock(2)));

        assertThat(service.adjustStock(bookId, -3)).isEqualTo(2);

        verify(bookPersistencePort).tryDecrementStock(bookId, 3);
        verify(bookPersistencePort, never()).incrementStock(any(), anyInt());
    }

    /**
     * The admin path must reach delisted books, so it goes through the unfiltered decrement rather
     * than the sellable one the checkout uses.
     */
    @Test
    void adjustStock_shouldNotUseTheSellableGuard() {
        when(bookPersistencePort.tryDecrementStock(bookId, 1)).thenReturn(true);
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(bookWithStock(0)));

        service.adjustStock(bookId, -1);

        verify(bookPersistencePort, never()).tryDecrementSellableStock(any(), anyInt());
    }

    @Test
    void adjustStock_shouldThrowInsufficientStock_whenDecrementWouldGoNegative() {
        when(bookPersistencePort.tryDecrementStock(bookId, 100)).thenReturn(false);
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(bookWithStock(5)));

        assertThatThrownBy(() -> service.adjustStock(bookId, -100))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("current=5");
    }

    @Test
    void adjustStock_shouldThrowBookNotFound_whenIncrementMatchesNoRow() {
        when(bookPersistencePort.incrementStock(bookId, 5)).thenReturn(false);

        assertThatThrownBy(() -> service.adjustStock(bookId, 5))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void adjustStock_shouldThrowBookNotFound_whenDecrementMatchesNoRowAndBookIsGone() {
        when(bookPersistencePort.tryDecrementStock(bookId, 1)).thenReturn(false);
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjustStock(bookId, -1))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void adjustStock_shouldWriteNothing_whenDeltaIsZero() {
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(bookWithStock(7)));

        assertThat(service.adjustStock(bookId, 0)).isEqualTo(7);

        verify(bookPersistencePort, never()).incrementStock(any(), anyInt());
        verify(bookPersistencePort, never()).tryDecrementStock(any(), anyInt());
    }

    private Book bookWithStock(int stockQuantity) {
        Instant now = Instant.now();
        return Book.reconstitute(
                BookId.of(bookId), "Clean Code", ISBN.of("9780132350884"), List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1st", null, Price.of(BigDecimal.TEN), stockQuantity,
                CategoryId.of(UUID.randomUUID()), null, 0.0, true, now, now);
    }
}
