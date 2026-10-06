package com.devrenno.bookland.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationItemEmbeddable {

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private int quantity;
}
