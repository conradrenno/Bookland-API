package com.devrenno.bookland.catalog.application.port.out;

import com.devrenno.bookland.catalog.domain.entity.StockReservation;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationPersistencePort {

    Optional<StockReservation> findByOrderId(UUID orderId);

    StockReservation save(StockReservation reservation);
}
