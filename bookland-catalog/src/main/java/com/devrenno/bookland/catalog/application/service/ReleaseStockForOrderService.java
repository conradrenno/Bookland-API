package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.ReleaseStockForOrderUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.StockReservationPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;

import java.util.UUID;

public class ReleaseStockForOrderService implements ReleaseStockForOrderUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final StockReservationPersistencePort reservationPersistencePort;

    private ReleaseStockForOrderService(BookPersistencePort bookPersistencePort,
                                        StockReservationPersistencePort reservationPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
        this.reservationPersistencePort = reservationPersistencePort;
    }

    public static ReleaseStockForOrderService create(BookPersistencePort bookPersistencePort,
                                                     StockReservationPersistencePort reservationPersistencePort) {
        return new ReleaseStockForOrderService(bookPersistencePort, reservationPersistencePort);
    }

    /**
     * Units go back only when the reservation flips from RESERVED to RELEASED, so a second release —
     * a redelivered message, or a cancellation after a compensation — finds nothing to return.
     */
    @Override
    public void release(UUID orderId) {
        reservationPersistencePort.findByOrderId(orderId).ifPresent(reservation -> {
            if (reservation.release()) {
                for (StockReservation.Item item : reservation.getItems()) {
                    bookPersistencePort.incrementStock(item.bookId(), item.quantity());
                }
                reservationPersistencePort.save(reservation);
            }
        });
    }
}
