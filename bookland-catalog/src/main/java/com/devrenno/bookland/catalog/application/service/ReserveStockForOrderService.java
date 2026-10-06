package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.StockReservationResult;
import com.devrenno.bookland.catalog.application.port.in.ReserveStockForOrderUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.StockReservationPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReserveStockForOrderService implements ReserveStockForOrderUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final StockReservationPersistencePort reservationPersistencePort;

    private ReserveStockForOrderService(BookPersistencePort bookPersistencePort,
                                        StockReservationPersistencePort reservationPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
        this.reservationPersistencePort = reservationPersistencePort;
    }

    public static ReserveStockForOrderService create(BookPersistencePort bookPersistencePort,
                                                     StockReservationPersistencePort reservationPersistencePort) {
        return new ReserveStockForOrderService(bookPersistencePort, reservationPersistencePort);
    }

    /**
     * All or nothing, without relying on a rollback: each line is taken with the same atomic,
     * guarded decrement a sale has always used, and when one comes up short the lines already taken
     * are put back before answering. Undoing by hand rather than by throwing keeps this correct in
     * whichever transaction the caller runs it in — the checkout's today, a message listener's once
     * the saga is asynchronous — since a throw inside a joined transaction would doom the caller's
     * other work along with it.
     */
    @Override
    public StockReservationResult reserve(UUID orderId, List<StockReservation.Item> items) {
        Optional<StockReservation> existing = reservationPersistencePort.findByOrderId(orderId);
        if (existing.isPresent()) {
            return existing.get().getStatus() == StockReservation.Status.FAILED
                    ? StockReservationResult.failure(List.of())
                    : StockReservationResult.success();
        }

        List<StockReservation.Item> taken = new ArrayList<>();
        List<UUID> unavailable = new ArrayList<>();
        for (StockReservation.Item item : items) {
            if (bookPersistencePort.tryDecrementSellableStock(item.bookId(), item.quantity())) {
                taken.add(item);
            } else {
                unavailable.add(item.bookId());
            }
        }

        if (!unavailable.isEmpty()) {
            taken.forEach(item -> bookPersistencePort.incrementStock(item.bookId(), item.quantity()));
            reservationPersistencePort.save(StockReservation.failed(orderId));
            return StockReservationResult.failure(unavailable);
        }

        reservationPersistencePort.save(StockReservation.reserved(orderId, items));
        return StockReservationResult.success();
    }
}
