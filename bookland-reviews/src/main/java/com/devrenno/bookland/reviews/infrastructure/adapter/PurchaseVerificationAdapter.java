package com.devrenno.bookland.reviews.infrastructure.adapter;

import com.devrenno.bookland.orders.application.port.in.VerifyPurchaseUseCase;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseVerificationAdapter implements PurchaseVerificationPort {

    private final VerifyPurchaseUseCase verifyPurchaseUseCase;

    @Override
    public boolean hasPurchasedBook(UUID customerId, UUID bookId) {
        return verifyPurchaseUseCase.hasDeliveredOrderWithBook(customerId, bookId);
    }
}
