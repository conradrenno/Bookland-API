package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.port.in.VerifyPurchaseUseCase;
import com.devrenno.bookland.orders.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class VerifyPurchaseService implements VerifyPurchaseUseCase {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public boolean hasDeliveredOrderWithBook(UUID customerId, UUID bookId) {
        return orderJpaRepository.existsByCustomerIdAndStatusAndItems_BookId(
                customerId, "DELIVERED", bookId
        );
    }
}
