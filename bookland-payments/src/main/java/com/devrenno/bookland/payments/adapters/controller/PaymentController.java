package com.devrenno.bookland.payments.adapters.controller;

import com.devrenno.bookland.payments.adapters.presenter.PaymentPresenter;
import com.devrenno.bookland.payments.adapters.viewmodel.PaymentViewModel;
import com.devrenno.bookland.payments.application.port.in.GetPaymentByOrderIdUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.application.service.GetPaymentByOrderIdService;

import java.util.UUID;

/**
 * Internal controller: orchestrates the payments query use case and delegates to the Presenter.
 * Also the module's composition root for HTTP delivery. Framework-free.
 * (ProcessPayment and Refund are cross-module boundary use cases exposed as beans, not routed here.)
 */
public class PaymentController {

    private final GetPaymentByOrderIdUseCase getPaymentByOrderIdUseCase;
    private final PaymentPresenter presenter;

    private PaymentController(GetPaymentByOrderIdUseCase getPaymentByOrderIdUseCase, PaymentPresenter presenter) {
        this.getPaymentByOrderIdUseCase = getPaymentByOrderIdUseCase;
        this.presenter = presenter;
    }

    public static PaymentController create(PaymentPersistencePort persistence) {
        return new PaymentController(GetPaymentByOrderIdService.create(persistence), PaymentPresenter.create());
    }

    public PaymentViewModel getByOrderId(UUID orderId) {
        return presenter.present(getPaymentByOrderIdUseCase.getByOrderId(orderId));
    }
}
