package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPaymentServiceTest {

    @Mock private PaymentPersistencePort persistence;
    @Mock private PaymentGatewayPort gateway;

    private RefundPaymentService service;

    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = RefundPaymentService.create(persistence, gateway);
    }

    @Test
    void refund_shouldReachTheGatewayAndMarkRefunded_whenThePaymentIsApproved() {
        Payment payment = payment(PaymentStatus.APPROVED);
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        service.refund(orderId);

        verify(gateway).refund("TXN-1");
        verify(persistence).save(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    /** A redelivered OrderCancelled finds the payment already refunded and must not pay out twice. */
    @Test
    void refund_shouldDoNothing_whenThePaymentWasAlreadyRefunded() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment(PaymentStatus.REFUNDED)));

        service.refund(orderId);

        verifyNoInteractions(gateway);
        verify(persistence, never()).save(any());
    }

    @Test
    void refund_shouldRefuse_whenThePaymentWasDeclined() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment(PaymentStatus.DECLINED)));

        assertThatThrownBy(() -> service.refund(orderId)).isInstanceOf(RefundNotAllowedException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void refund_shouldRefuse_whenTheOrderWasNeverCharged() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(orderId)).isInstanceOf(PaymentNotFoundException.class);
        verifyNoInteractions(gateway);
    }

    private Payment payment(PaymentStatus status) {
        return Payment.create(orderId, UUID.randomUUID(), BigDecimal.valueOf(50), PaymentMethod.PIX, status,
                "TXN-1", null);
    }
}
