package com.devrenno.bookland.payments.infrastructure.gateway;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public PaymentResult charge(ProcessPaymentCommand command) {
        String transactionId = "SIM-" + UUID.randomUUID();
        return new PaymentResult(true, transactionId, null);
    }

    @Override
    public void refund(String transactionId) {
        // Simulated: log would go here in a real implementation
    }
}
