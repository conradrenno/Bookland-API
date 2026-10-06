package com.devrenno.bookland.payments.infrastructure.gateway;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stands in for a payment provider. It approves every charge up to
 * {@code bookland.payments.simulated.decline-above} and declines anything above it — a rule a test,
 * or a person in the Swagger UI, can trigger on purpose. Without a way to be declined, the checkout's
 * compensation (giving the reserved stock back) would never run outside a mock.
 */
@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    static final String DECLINE_REASON = "Amount above the simulated card limit";

    private final BigDecimal declineAbove;

    public SimulatedPaymentGatewayAdapter(
            @Value("${bookland.payments.simulated.decline-above:1000.00}") BigDecimal declineAbove) {
        this.declineAbove = declineAbove;
    }

    @Override
    public PaymentResult charge(ProcessPaymentCommand command) {
        if (command.amount().compareTo(declineAbove) > 0) {
            return new PaymentResult(false, null, DECLINE_REASON);
        }
        return new PaymentResult(true, "SIM-" + UUID.randomUUID(), null);
    }

    @Override
    public void refund(String transactionId) {
        // Simulated: log would go here in a real implementation
    }
}
