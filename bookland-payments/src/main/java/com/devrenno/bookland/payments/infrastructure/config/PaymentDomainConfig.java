package com.devrenno.bookland.payments.infrastructure.config;

import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.application.service.GetPaymentByOrderIdService;
import com.devrenno.bookland.payments.application.service.ProcessPaymentService;
import com.devrenno.bookland.payments.application.service.RefundPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentDomainConfig {

    @Bean
    public ProcessPaymentService processPaymentService(PaymentGatewayPort gateway, PaymentPersistencePort persistence) {
        return new ProcessPaymentService(gateway, persistence);
    }

    @Bean
    public RefundPaymentService refundPaymentService(PaymentPersistencePort persistence, PaymentGatewayPort gateway) {
        return new RefundPaymentService(persistence, gateway);
    }

    @Bean
    public GetPaymentByOrderIdService getPaymentByOrderIdService(PaymentPersistencePort persistence) {
        return new GetPaymentByOrderIdService(persistence);
    }
}
