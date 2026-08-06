package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementBookStockServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;

    private IncrementBookStockService service;

    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = IncrementBookStockService.create(bookPersistencePort);
    }

    @Test
    void increment_shouldReturnUnitsToTheBook() {
        when(bookPersistencePort.incrementStock(bookId, 2)).thenReturn(true);

        assertThatCode(() -> service.increment(bookId, 2)).doesNotThrowAnyException();

        verify(bookPersistencePort).incrementStock(bookId, 2);
    }

    /**
     * Soft deletion keeps the row, so no matching row means the id never existed — failing the
     * caller beats discarding the units silently.
     */
    @Test
    void increment_shouldThrowBookNotFound_whenNoRowMatched() {
        when(bookPersistencePort.incrementStock(bookId, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.increment(bookId, 1))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void increment_shouldNotReadTheBookBeforeIncrementing() {
        when(bookPersistencePort.incrementStock(bookId, 1)).thenReturn(true);

        service.increment(bookId, 1);

        verify(bookPersistencePort, never()).findById(any());
    }

    @Test
    void increment_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> service.increment(bookId, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.increment(bookId, -1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(bookPersistencePort, never()).incrementStock(any(), anyInt());
    }
}
