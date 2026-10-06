package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.StockReservationResult;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.StockReservationPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReserveStockForOrderServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;
    @Mock private StockReservationPersistencePort reservationPersistencePort;

    private ReserveStockForOrderService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID bookA = UUID.randomUUID();
    private final UUID bookB = UUID.randomUUID();
    private final List<StockReservation.Item> items =
            List.of(new StockReservation.Item(bookA, 2), new StockReservation.Item(bookB, 1));

    @BeforeEach
    void setUp() {
        service = ReserveStockForOrderService.create(bookPersistencePort, reservationPersistencePort);
    }

    @Test
    @DisplayName("every line available: all taken and the reservation recorded as RESERVED")
    void reservesEveryLine() {
        when(reservationPersistencePort.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(bookPersistencePort.tryDecrementSellableStock(any(), anyInt())).thenReturn(true);

        StockReservationResult result = service.reserve(orderId, items);

        assertThat(result.reserved()).isTrue();
        ArgumentCaptor<StockReservation> saved = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservationPersistencePort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(StockReservation.Status.RESERVED);
        assertThat(saved.getValue().getItems()).containsExactlyElementsOf(items);
    }

    /** All or nothing: the line already taken goes back, so a failed reservation holds nothing. */
    @Test
    @DisplayName("one line short: the lines already taken are put back and FAILED is recorded")
    void putsBackWhatItTookWhenOneLineIsShort() {
        when(reservationPersistencePort.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(bookPersistencePort.tryDecrementSellableStock(bookA, 2)).thenReturn(true);
        when(bookPersistencePort.tryDecrementSellableStock(bookB, 1)).thenReturn(false);

        StockReservationResult result = service.reserve(orderId, items);

        assertThat(result.reserved()).isFalse();
        assertThat(result.unavailableBookIds()).containsExactly(bookB);
        verify(bookPersistencePort).incrementStock(bookA, 2);
        verify(bookPersistencePort, never()).incrementStock(bookB, 1);
        ArgumentCaptor<StockReservation> saved = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservationPersistencePort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(StockReservation.Status.FAILED);
    }

    /** A repeated request — a redelivered message — answers from the record and touches no stock. */
    @Test
    @DisplayName("asked again after reserving: same answer, stock untouched")
    void repeatedRequestAfterSuccessChangesNothing() {
        when(reservationPersistencePort.findByOrderId(orderId))
                .thenReturn(Optional.of(StockReservation.reserved(orderId, items)));

        assertThat(service.reserve(orderId, items).reserved()).isTrue();
        verifyNoInteractions(bookPersistencePort);
        verify(reservationPersistencePort, never()).save(any());
    }

    /**
     * Without the FAILED record, a duplicate arriving after the failure would try again and might
     * succeed — reserving units for an order that was already rejected.
     */
    @Test
    @DisplayName("asked again after failing: still a failure, no second attempt")
    void repeatedRequestAfterFailureDoesNotRetry() {
        when(reservationPersistencePort.findByOrderId(orderId))
                .thenReturn(Optional.of(StockReservation.failed(orderId)));

        assertThat(service.reserve(orderId, items).reserved()).isFalse();
        verifyNoInteractions(bookPersistencePort);
    }
}
