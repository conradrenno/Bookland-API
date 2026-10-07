package com.devrenno.bookland.payments.infrastructure.config;

import com.devrenno.bookland.payments.adapters.controller.PaymentController;
import com.devrenno.bookland.payments.application.dto.RetryPolicy;
import com.devrenno.bookland.payments.application.port.in.ProcessPendingPaymentsUseCase;
import com.devrenno.bookland.payments.application.port.in.RequestChargeUseCase;
import com.devrenno.bookland.payments.application.port.in.RequestRefundUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.application.port.out.PaymentReplyPort;
import com.devrenno.bookland.payments.application.port.out.TransactionPort;
import com.devrenno.bookland.payments.application.service.ProcessPendingPaymentsService;
import com.devrenno.bookland.payments.application.service.RequestChargeService;
import com.devrenno.bookland.payments.application.service.RequestRefundService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Composition root of the payments module. Exposes the internal PaymentController (HTTP delivery)
 * and the use cases the module's own messaging and gateway worker drive. No other module calls
 * payments in process any more: charges and refunds arrive as messages.
 */
@Configuration
public class PaymentBeansConfig {

    private static final Clock CLOCK = Clock.systemUTC();

    /** Internal controller = HTTP-delivery entry point (getByOrderId). */
    @Bean
    public PaymentController paymentController(PaymentPersistencePort persistence) {
        return PaymentController.create(persistence);
    }

    /** Consumed by this module's PaymentCommandListener, for the checkout saga's ChargePayment. */
    @Bean
    public RequestChargeUseCase requestChargeUseCase(PaymentPersistencePort persistence) {
        return RequestChargeService.create(persistence, CLOCK);
    }

    /**
     * Consumed by this module's OrderCancelledListener, when orders announces a cancellation, and by
     * nothing else. Deliberately not routed: a refund on its own leaves the order CONFIRMED and the stock
     * short, which is the half-operation an admin endpoint used to expose.
     */
    @Bean
    public RequestRefundUseCase requestRefundUseCase(PaymentPersistencePort persistence) {
        return RequestRefundService.create(persistence, CLOCK);
    }

    /** Driven by the PaymentGatewayWorker. */
    @Bean
    public ProcessPendingPaymentsUseCase processPendingPaymentsUseCase(
            PaymentPersistencePort persistence, PaymentGatewayPort gateway, PaymentReplyPort replyPort,
            TransactionPort transactionPort,
            @Value("${bookland.payments.gateway.retry.initial-delay:1s}") Duration initialDelay,
            @Value("${bookland.payments.gateway.retry.max-delay:1m}") Duration maxDelay) {
        return ProcessPendingPaymentsService.create(persistence, gateway, replyPort, transactionPort,
                new RetryPolicy(initialDelay, maxDelay), CLOCK);
    }
}
