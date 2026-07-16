package com.devrenno.bookland.payments.adapters.presenter;

import com.devrenno.bookland.payments.adapters.viewmodel.PaymentViewModel;
import com.devrenno.bookland.payments.domain.entity.Payment;

/**
 * Transforms a domain Payment into the delivery-facing PaymentViewModel. Plain Java.
 */
public class PaymentPresenter {

    private PaymentPresenter() {
    }

    public static PaymentPresenter create() {
        return new PaymentPresenter();
    }

    public PaymentViewModel present(Payment payment) {
        return new PaymentViewModel(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getGatewayTransactionId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
