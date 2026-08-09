package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import com.devrenno.bookland.payments.domain.exception.PaymentAccessDeniedException;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The route took only the order id, so any authenticated customer could read any order's payment —
 * amount, method, customer id and the gateway transaction id — by changing one UUID in the path.
 */
@ExtendWith(MockitoExtension.class)
class GetPaymentByOrderIdServiceTest {

    @Mock private PaymentPersistencePort persistence;

    private GetPaymentByOrderIdService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();
    private final UUID intruder = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = GetPaymentByOrderIdService.create(persistence);
    }

    private Payment payment() {
        return Payment.reconstitute(UUID.randomUUID(), orderId, owner, new BigDecimal("59.90"),
                PaymentMethod.CREDIT_CARD, PaymentStatus.APPROVED, "gw-tx-1",
                Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("the customer who paid reads their own payment")
    void ownerReadsTheirPayment() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment()));

        Payment result = service.getByOrderId(orderId, owner);

        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getGatewayTransactionId()).isEqualTo("gw-tx-1");
    }

    @Test
    @DisplayName("another customer is denied, not served")
    void anotherCustomerIsDenied() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.of(payment()));

        assertThatThrownBy(() -> service.getByOrderId(orderId, intruder))
                .isInstanceOf(PaymentAccessDeniedException.class);
    }

    @Test
    @DisplayName("a missing payment is still a 404, not a 403")
    void missingPaymentIsNotFound() {
        when(persistence.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByOrderId(orderId, owner))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
