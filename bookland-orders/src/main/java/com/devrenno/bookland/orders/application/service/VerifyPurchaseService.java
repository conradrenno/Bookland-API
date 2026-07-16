package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.VerifyPurchaseUseCase;
import com.devrenno.bookland.orders.application.port.out.PurchaseVerificationPort;

import java.util.UUID;

public class VerifyPurchaseService implements VerifyPurchaseUseCase {

    private final PurchaseVerificationPort purchaseVerificationPort;

    private VerifyPurchaseService(PurchaseVerificationPort purchaseVerificationPort) {
        this.purchaseVerificationPort = purchaseVerificationPort;
    }

    public static VerifyPurchaseService create(PurchaseVerificationPort purchaseVerificationPort) {
        return new VerifyPurchaseService(purchaseVerificationPort);
    }

    @Override
    public boolean hasDeliveredOrderWithBook(UUID customerId, UUID bookId) {
        return purchaseVerificationPort.existsDeliveredOrderWithBook(customerId, bookId);
    }
}
