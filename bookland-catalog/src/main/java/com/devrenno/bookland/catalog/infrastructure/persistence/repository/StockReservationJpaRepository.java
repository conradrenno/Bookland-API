package com.devrenno.bookland.catalog.infrastructure.persistence.repository;

import com.devrenno.bookland.catalog.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationJpaEntity, UUID> {
}
