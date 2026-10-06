package com.devrenno.bookland.catalog.infrastructure.persistence.adapter;

import com.devrenno.bookland.catalog.application.port.out.StockReservationPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.StockReservationItemEmbeddable;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.StockReservationJpaEntity;
import com.devrenno.bookland.catalog.infrastructure.persistence.repository.StockReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockReservationPersistenceAdapter implements StockReservationPersistencePort {

    private final StockReservationJpaRepository repository;

    @Override
    public Optional<StockReservation> findByOrderId(UUID orderId) {
        return repository.findById(orderId).map(StockReservationPersistenceAdapter::toDomain);
    }

    @Override
    public StockReservation save(StockReservation reservation) {
        return toDomain(repository.save(toEntity(reservation)));
    }

    private static StockReservationJpaEntity toEntity(StockReservation reservation) {
        return StockReservationJpaEntity.builder()
                .orderId(reservation.getOrderId())
                .status(reservation.getStatus().name())
                .items(new ArrayList<>(reservation.getItems().stream()
                        .map(item -> new StockReservationItemEmbeddable(item.bookId(), item.quantity()))
                        .toList()))
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }

    private static StockReservation toDomain(StockReservationJpaEntity entity) {
        return StockReservation.reconstitute(
                entity.getOrderId(),
                entity.getItems().stream()
                        .map(item -> new StockReservation.Item(item.getBookId(), item.getQuantity()))
                        .toList(),
                StockReservation.Status.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
