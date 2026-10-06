package com.devrenno.bookland.payments.infrastructure.gateway;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayAdapterTest {

    private final SimulatedPaymentGatewayAdapter gateway = new SimulatedPaymentGatewayAdapter(new BigDecimal("1000.00"));

    @Test
    @DisplayName("up to the limit, inclusive, the charge is approved")
    void approvesUpToTheLimit() {
        PaymentResult result = gateway.charge(command("1000.00"));

        assertThat(result.approved()).isTrue();
        assertThat(result.transactionId()).startsWith("SIM-");
    }

    @Test
    @DisplayName("above the limit the charge is declined, with a reason")
    void declinesAboveTheLimit() {
        PaymentResult result = gateway.charge(command("1000.01"));

        assertThat(result.approved()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.declineReason()).isEqualTo(SimulatedPaymentGatewayAdapter.DECLINE_REASON);
    }

    private static ProcessPaymentCommand command(String amount) {
        return new ProcessPaymentCommand(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(amount),
                PaymentMethod.CREDIT_CARD);
    }
}
