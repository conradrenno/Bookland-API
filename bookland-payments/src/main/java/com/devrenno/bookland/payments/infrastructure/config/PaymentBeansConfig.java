package com.devrenno.bookland.payments.infrastructure.config;

import com.devrenno.bookland.payments.adapters.controller.PaymentController;
import com.devrenno.bookland.payments.application.port.in.ProcessPaymentUseCase;
import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.application.service.ProcessPaymentService;
import com.devrenno.bookland.payments.application.service.RefundPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the payments module. Exposes the internal PaymentController (HTTP delivery)
 * plus the cross-module boundary use cases consumed by orders (process, refund).
 */
@Configuration
public class PaymentBeansConfig {

    /** Internal controller = HTTP-delivery entry point (getByOrderId). */
    @Bean
    public PaymentController paymentController(PaymentPersistencePort persistence) {
        return PaymentController.create(persistence);
    }

    /** Consumed by this module's PaymentCommandListener, for the checkout saga's ChargePayment. */
    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(PaymentGatewayPort gateway, PaymentPersistencePort persistence) {
        return ProcessPaymentService.create(gateway, persistence);
    }

    /**
     * Consumed by this module's OrderCancelledListener, when orders announces a cancellation, and by
     * nothing else. Deliberately not routed: a refund on its own leaves the order CONFIRMED and the stock
     * short, which is the half-operation an admin endpoint used to expose.
     */
    @Bean
    public RefundPaymentUseCase refundPaymentUseCase(PaymentPersistencePort persistence, PaymentGatewayPort gateway) {
        return RefundPaymentService.create(persistence, gateway);
    }
}
