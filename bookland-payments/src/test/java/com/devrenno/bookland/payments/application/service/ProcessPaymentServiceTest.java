package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock private PaymentGatewayPort gateway;
    @Mock private PaymentPersistencePort persistence;

    private ProcessPaymentService service;

    @BeforeEach
    void setUp() {
        service = ProcessPaymentService.create(gateway, persistence);
    }

    @Test
    void processPayment_shouldPersistApprovedPayment_whenGatewayApproves() {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(50), PaymentMethod.CREDIT_CARD);
        when(gateway.charge(command)).thenReturn(new PaymentResult(true, "TXN-1", null));

        PaymentResult result = service.processPayment(command);

        assertThat(result.approved()).isTrue();
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(persistence).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(captor.getValue().getGatewayTransactionId()).isEqualTo("TXN-1");
    }

    @Test
    void processPayment_shouldPersistDeclinedPayment_whenGatewayDeclines() {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(50), PaymentMethod.CREDIT_CARD);
        when(gateway.charge(command)).thenReturn(new PaymentResult(false, "TXN-2", "Insufficient funds"));

        PaymentResult result = service.processPayment(command);

        assertThat(result.approved()).isFalse();
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(persistence).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }
}
