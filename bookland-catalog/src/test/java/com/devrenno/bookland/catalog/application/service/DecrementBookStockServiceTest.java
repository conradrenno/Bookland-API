package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecrementBookStockServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;

    private DecrementBookStockService service;

    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = DecrementBookStockService.create(bookPersistencePort);
    }

    @Test
    void tryDecrement_shouldReturnTrue_whenTheStoreAppliedTheDecrement() {
        when(bookPersistencePort.tryDecrementSellableStock(bookId, 2)).thenReturn(true);

        assertThat(service.tryDecrement(bookId, 2)).isTrue();
    }

    @Test
    void tryDecrement_shouldReturnFalse_whenTheStoreRefusedTheDecrement() {
        when(bookPersistencePort.tryDecrementSellableStock(bookId, 2)).thenReturn(false);

        assertThat(service.tryDecrement(bookId, 2)).isFalse();
    }

    /**
     * The service must not read the book to pre-check availability: a check against a separate
     * snapshot is exactly the read-modify-write pattern this use case exists to avoid.
     */
    @Test
    void tryDecrement_shouldNotReadTheBookBeforeDecrementing() {
        when(bookPersistencePort.tryDecrementSellableStock(bookId, 1)).thenReturn(true);

        service.tryDecrement(bookId, 1);

        verify(bookPersistencePort, never()).findById(any());
    }

    /**
     * Selling goes through the {@code active}-filtered guard, never the unfiltered one the admin
     * correction path uses — a delisted book must not be sold even when stock remains.
     */
    @Test
    void tryDecrement_shouldUseTheSellableGuard() {
        when(bookPersistencePort.tryDecrementSellableStock(bookId, 1)).thenReturn(true);

        service.tryDecrement(bookId, 1);

        verify(bookPersistencePort, never()).tryDecrementStock(any(), anyInt());
    }

    @Test
    void tryDecrement_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> service.tryDecrement(bookId, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.tryDecrement(bookId, -1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(bookPersistencePort, never()).tryDecrementStock(any(), anyInt());
    }
}
