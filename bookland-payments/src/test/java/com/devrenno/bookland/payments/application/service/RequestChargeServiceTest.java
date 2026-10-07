package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestChargeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");

    @Mock private PaymentPersistencePort persistence;

    private RequestChargeService service;

    private final ProcessPaymentCommand command = new ProcessPaymentCommand(
            UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(50), PaymentMethod.CREDIT_CARD);

    @BeforeEach
    void setUp() {
        service = RequestChargeService.create(persistence, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void requestCharge_shouldRecordAPendingCharge_dueAtOnce() {
        when(persistence.findByOrderId(command.orderId())).thenReturn(Optional.empty());

        service.requestCharge(command);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(persistence).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getValue().getNextAttemptAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("50");
    }

    /** A second command for the same order — a redelivery with a new message id — records nothing. */
    @Test
    void requestCharge_shouldDoNothing_whenTheOrderAlreadyHasAPayment() {
        when(persistence.findByOrderId(command.orderId())).thenReturn(Optional.of(mock(Payment.class)));

        service.requestCharge(command);

        verify(persistence, never()).save(any());
    }
}
