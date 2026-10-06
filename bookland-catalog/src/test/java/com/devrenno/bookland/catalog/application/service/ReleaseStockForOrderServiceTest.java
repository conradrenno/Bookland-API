package com.devrenno.bookland.catalog.application.service;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseStockForOrderServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;
    @Mock private StockReservationPersistencePort reservationPersistencePort;

    private ReleaseStockForOrderService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = ReleaseStockForOrderService.create(bookPersistencePort, reservationPersistencePort);
    }

    @Test
    @DisplayName("a held reservation gives its units back and is marked RELEASED")
    void releasesAHeldReservation() {
        when(reservationPersistencePort.findByOrderId(orderId)).thenReturn(Optional.of(
                StockReservation.reserved(orderId, List.of(new StockReservation.Item(bookId, 3)))));

        service.release(orderId);

        verify(bookPersistencePort).incrementStock(bookId, 3);
        ArgumentCaptor<StockReservation> saved = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservationPersistencePort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(StockReservation.Status.RELEASED);
    }

    /** The second release of the same order — a redelivery, or cancel after compensation — is a no-op. */
    @Test
    @DisplayName("releasing twice returns the units once")
    void secondReleaseReturnsNothing() {
        StockReservation reservation =
                StockReservation.reserved(orderId, List.of(new StockReservation.Item(bookId, 3)));
        reservation.release();
        when(reservationPersistencePort.findByOrderId(orderId)).thenReturn(Optional.of(reservation));

        service.release(orderId);

        verifyNoInteractions(bookPersistencePort);
        verify(reservationPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("an order that never reserved anything releases nothing")
    void unknownOrderIsANoOp() {
        when(reservationPersistencePort.findByOrderId(orderId)).thenReturn(Optional.empty());

        service.release(orderId);

        verifyNoInteractions(bookPersistencePort);
    }

    @Test
    @DisplayName("a failed reservation holds nothing to give back")
    void failedReservationIsANoOp() {
        when(reservationPersistencePort.findByOrderId(orderId)).thenReturn(Optional.of(StockReservation.failed(orderId)));

        service.release(orderId);

        verifyNoInteractions(bookPersistencePort);
    }
}
