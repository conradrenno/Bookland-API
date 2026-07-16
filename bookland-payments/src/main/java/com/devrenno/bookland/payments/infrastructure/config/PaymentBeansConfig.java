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
 * plus the cross-module boundary use cases consumed by orders (process) and by orders + the admin
 * refund endpoint (refund).
 */
@Configuration
public class PaymentBeansConfig {

    /** Internal controller = HTTP-delivery entry point (getByOrderId). */
    @Bean
    public PaymentController paymentController(PaymentPersistencePort persistence) {
        return PaymentController.create(persistence);
    }

    /** Cross-module: consumed by orders (PaymentAdapter) during checkout. */
    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(PaymentGatewayPort gateway, PaymentPersistencePort persistence) {
        return ProcessPaymentService.create(gateway, persistence);
    }

    /** Cross-module: consumed by orders (RefundAdapter) and the admin refund endpoint. */
    @Bean
    public RefundPaymentUseCase refundPaymentUseCase(PaymentPersistencePort persistence, PaymentGatewayPort gateway) {
        return RefundPaymentService.create(persistence, gateway);
    }
}
