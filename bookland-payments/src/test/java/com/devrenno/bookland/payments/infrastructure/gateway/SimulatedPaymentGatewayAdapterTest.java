package com.devrenno.bookland.payments.infrastructure.gateway;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayUnavailableException;
import com.devrenno.bookland.payments.application.port.out.RefundRejectedException;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedPaymentGatewayAdapterTest {

    private final SimulatedPaymentGatewayAdapter gateway = new SimulatedPaymentGatewayAdapter(new BigDecimal("1000.00"));

    @Test
    @DisplayName("up to the limit, inclusive, the charge is approved")
    void approvesUpToTheLimit() {
        PaymentResult result = gateway.charge("k1", command("1000.00"));

        assertThat(result.approved()).isTrue();
        assertThat(result.transactionId()).startsWith("SIM-");
    }

    @Test
    @DisplayName("above the limit the charge is declined, with a reason")
    void declinesAboveTheLimit() {
        PaymentResult result = gateway.charge("k1", command("1000.01"));

        assertThat(result.approved()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.declineReason()).isEqualTo(SimulatedPaymentGatewayAdapter.DECLINE_REASON);
    }

    @Test
    @DisplayName("the same idempotency key gets the first answer back, and charges once")
    void repeatedKeyChargesOnce() {
        ProcessPaymentCommand command = command("10.00");

        PaymentResult first = gateway.charge("charge:x", command);
        PaymentResult second = gateway.charge("charge:x", command);

        assertThat(second).isEqualTo(first);
        assertThat(gateway.chargesMade()).isEqualTo(1);
    }

    @Test
    @DisplayName("an outage gives no answer for the given number of calls, then the gateway answers")
    void outageThenAnswer() {
        ProcessPaymentCommand command = command("10.00");
        gateway.failNextCharges(command.customerId(), 2);

        assertThatThrownBy(() -> gateway.charge("k", command)).isInstanceOf(PaymentGatewayUnavailableException.class);
        assertThatThrownBy(() -> gateway.charge("k", command)).isInstanceOf(PaymentGatewayUnavailableException.class);
        assertThat(gateway.charge("k", command).approved()).isTrue();
    }

    @Test
    @DisplayName("refunds: a repeated key refunds once; a refused order is refused every time")
    void refunds() {
        UUID orderId = UUID.randomUUID();
        gateway.refund("refund:a", orderId, "SIM-1");
        gateway.refund("refund:a", orderId, "SIM-1");
        assertThat(gateway.refundsMade()).isEqualTo(1);

        UUID refused = UUID.randomUUID();
        gateway.refuseRefunds(refused);
        assertThatThrownBy(() -> gateway.refund("refund:b", refused, "SIM-2"))
                .isInstanceOf(RefundRejectedException.class);
    }

    private static ProcessPaymentCommand command(String amount) {
        return new ProcessPaymentCommand(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(amount),
                PaymentMethod.CREDIT_CARD);
    }
}
