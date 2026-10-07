package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestRefundServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");

    @Mock private PaymentPersistencePort persistence;

    private RequestRefundService service;

    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = RequestRefundService.create(persistence, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Only the intention is recorded; the gateway is the worker's business. */
    @Test
    void requestRefund_shouldMarkAnApprovedPaymentRefundPending_dueAtOnce() {
        Payment payment = payment(PaymentStatus.APPROVED);
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        service.requestRefund(orderId);

        verify(persistence).save(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getNextAttemptAt()).isEqualTo(NOW);
    }

    /** A redelivered OrderCancelled finds the refund already requested, done or refused. */
    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"REFUND_PENDING", "REFUNDED", "REFUND_FAILED"})
    void requestRefund_shouldChangeNothing_whenTheRefundWasAlreadyRequested(PaymentStatus status) {
        Payment payment = payment(status);
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        service.requestRefund(orderId);

        verify(persistence, never()).save(any());
        assertThat(payment.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PENDING", "DECLINED"})
    void requestRefund_shouldRefuse_whenNoMoneyWasTaken(PaymentStatus status) {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment(status)));

        assertThatThrownBy(() -> service.requestRefund(orderId)).isInstanceOf(RefundNotAllowedException.class);
    }

    @Test
    void requestRefund_shouldRefuse_whenTheOrderWasNeverCharged() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestRefund(orderId)).isInstanceOf(PaymentNotFoundException.class);
    }

    private Payment payment(PaymentStatus status) {
        return Payment.reconstitute(UUID.randomUUID(), orderId, UUID.randomUUID(), BigDecimal.valueOf(50),
                PaymentMethod.PIX, status, "TXN-1", null, 0, null, null, NOW, NOW);
    }
}
