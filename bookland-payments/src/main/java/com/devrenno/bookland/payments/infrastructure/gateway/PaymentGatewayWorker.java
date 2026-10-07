package com.devrenno.bookland.payments.infrastructure.gateway;

import com.devrenno.bookland.payments.application.port.in.ProcessPendingPaymentsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Every second, takes the charges and refunds that are due to the gateway. The decisions (what to
 * call, what an answer means, when to retry) are in {@code ProcessPendingPaymentsService}; this class
 * only runs it on a schedule and keeps one payment's failure from stopping the others.
 *
 * <p>Runs because {@code PaymentsOutboxConfig} turns on {@code @EnableScheduling} for the module.
 */
@Component
public class PaymentGatewayWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayWorker.class);
    private static final int BATCH = 50;

    private final ProcessPendingPaymentsUseCase processPendingPayments;

    public PaymentGatewayWorker(ProcessPendingPaymentsUseCase processPendingPayments) {
        this.processPendingPayments = processPendingPayments;
    }

    @Scheduled(fixedDelay = 1000)
    public void run() {
        for (UUID paymentId : processPendingPayments.findDue(BATCH)) {
            try {
                processPendingPayments.process(paymentId);
            } catch (RuntimeException e) {
                // Recording the outcome failed (the database, typically). The payment is still due and
                // is taken again on the next run, with the same idempotency key.
                log.warn("Payment {} not settled with the gateway yet, will retry: {}", paymentId, e.getMessage());
            }
        }
    }
}
