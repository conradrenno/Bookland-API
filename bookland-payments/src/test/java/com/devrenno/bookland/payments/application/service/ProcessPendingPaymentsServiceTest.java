package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.dto.RetryPolicy;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayUnavailableException;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.application.port.out.PaymentReplyPort;
import com.devrenno.bookland.payments.application.port.out.RefundRejectedException;
import com.devrenno.bookland.payments.application.port.out.TransactionPort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The worker against in-memory fakes: a gateway that honours idempotency keys like a real provider,
 * and a clock the test moves forward. What matters here is the sequence of states over time, which a
 * mock would only restate.
 */
class ProcessPendingPaymentsServiceTest {

    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-07T12:00:00Z"));
    private final InMemoryPayments payments = new InMemoryPayments();
    private final KeyedGateway gateway = new KeyedGateway();
    private final RecordedReplies replies = new RecordedReplies();
    private final FlakyTransactions transactions = new FlakyTransactions();

    private ProcessPendingPaymentsService service;
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = ProcessPendingPaymentsService.create(payments, gateway, replies, transactions,
                new RetryPolicy(Duration.ofSeconds(1), Duration.ofSeconds(3)), clock);
    }

    @Test
    @DisplayName("an approved charge is recorded and answered to the saga")
    void approvedCharge() {
        Payment payment = pendingCharge("50.00");

        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getGatewayTransactionId()).isNotBlank();
        assertThat(payment.getNextAttemptAt()).isNull();
        assertThat(replies.sent).containsExactly("approved:" + orderId);
    }

    @Test
    @DisplayName("a declined charge is recorded and answered to the saga with the reason")
    void declinedCharge() {
        Payment payment = pendingCharge("5000.00");

        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(replies.sent).containsExactly("declined:" + orderId + ":over the limit");
    }

    /** No answer is not a decline: the charge waits, and the saga hears nothing yet. */
    @Test
    @DisplayName("no answer from the gateway: still PENDING, retried later, nothing sent to the saga")
    void noAnswerLeavesTheChargePending() {
        Payment payment = pendingCharge("50.00");
        gateway.outages = 1;

        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAttempts()).isEqualTo(1);
        assertThat(payment.getLastError()).contains("PaymentGatewayUnavailableException");
        assertThat(payment.getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(1));
        assertThat(replies.sent).isEmpty();
        assertThat(service.findDue(10)).as("not due before the retry time").isEmpty();

        clock.advance(Duration.ofSeconds(1));
        assertThat(service.findDue(10)).containsExactly(payment.getId());
        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(replies.sent).containsExactly("approved:" + orderId);
    }

    @Test
    @DisplayName("the wait doubles after each failure, up to the ceiling")
    void backoffDoublesUpToTheCeiling() {
        Payment payment = pendingCharge("50.00");
        gateway.outages = 4;
        List<Duration> waits = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Instant before = clock.instant();
            service.process(payment.getId());
            waits.add(Duration.between(before, payment.getNextAttemptAt()));
            clock.advance(waits.getLast());
        }

        assertThat(waits).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3),
                Duration.ofSeconds(3));
        assertThat(payment.getAttempts()).isEqualTo(4);
    }

    /**
     * The case the idempotency key exists for: the gateway charged, and recording it failed. The
     * payment is still PENDING, so the worker calls again — with the same key, so the gateway gives
     * the first answer back instead of charging twice.
     */
    @Test
    @DisplayName("the gateway charged but recording it failed: the retry does not charge twice")
    void retryAfterALostAnswerDoesNotChargeTwice() {
        Payment payment = pendingCharge("50.00");
        transactions.failNext = true;

        assertThatThrownBy(() -> service.process(payment.getId())).isInstanceOf(IllegalStateException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        service.process(payment.getId());

        assertThat(gateway.calls).containsExactly("charge:" + orderId, "charge:" + orderId);
        assertThat(gateway.chargesMade).as("one real charge").isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(replies.sent).containsExactly("approved:" + orderId);
    }

    @Test
    @DisplayName("a pending refund is made with its own key and marked REFUNDED")
    void refund() {
        Payment payment = pendingRefund();

        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(gateway.calls).containsExactly("refund:" + orderId);
    }

    @Test
    @DisplayName("no answer to a refund: still REFUND_PENDING, retried later")
    void refundWithoutAnswerStaysPending() {
        Payment payment = pendingRefund();
        gateway.outages = 1;

        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getAttempts()).isEqualTo(1);
    }

    /** The gateway's "no" is final: REFUND_FAILED, out of the worker's queue, the reason kept. */
    @Test
    @DisplayName("a refund the gateway refuses for good: REFUND_FAILED, no more attempts")
    void refusedRefund() {
        Payment payment = pendingRefund();
        gateway.refuseRefunds = true;

        service.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);
        assertThat(payment.getLastError()).isEqualTo("window closed");
        assertThat(payment.getNextAttemptAt()).isNull();
        assertThat(service.findDue(10)).isEmpty();
    }

    @Test
    @DisplayName("a payment already settled is not taken to the gateway")
    void settledPaymentIsLeftAlone() {
        Payment payment = pendingCharge("50.00");
        service.process(payment.getId());
        gateway.calls.clear();

        service.process(payment.getId());

        assertThat(gateway.calls).isEmpty();
    }

    private Payment pendingCharge(String amount) {
        Payment payment = Payment.requestCharge(orderId, UUID.randomUUID(), new BigDecimal(amount),
                PaymentMethod.PIX, clock.instant());
        return payments.save(payment);
    }

    private Payment pendingRefund() {
        Payment payment = Payment.requestCharge(orderId, UUID.randomUUID(), new BigDecimal("50.00"),
                PaymentMethod.PIX, clock.instant());
        payment.approve("TXN-1", clock.instant());
        payment.requestRefund(clock.instant());
        return payments.save(payment);
    }

    // --- fakes ---

    private static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Keeps the same instances, so the test reads the state the service left. */
    private static final class InMemoryPayments implements PaymentPersistencePort {
        private final Map<UUID, Payment> byId = new HashMap<>();

        @Override
        public Payment save(Payment payment) {
            byId.put(payment.getId(), payment);
            return payment;
        }

        @Override
        public Optional<Payment> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Payment> findByOrderId(UUID orderId) {
            return byId.values().stream().filter(p -> p.getOrderId().equals(orderId)).findFirst();
        }

        @Override
        public List<Payment> findDue(Instant now, int limit) {
            return byId.values().stream()
                    .filter(p -> p.awaitsGateway() && !p.getNextAttemptAt().isAfter(now))
                    .sorted(Comparator.comparing(Payment::getNextAttemptAt))
                    .limit(limit)
                    .toList();
        }
    }

    /** Answers a repeated key with the first answer, as a real provider does. */
    private static final class KeyedGateway implements PaymentGatewayPort {
        final List<String> calls = new ArrayList<>();
        final Map<String, PaymentResult> answers = new HashMap<>();
        int chargesMade;
        int outages;
        boolean refuseRefunds;

        @Override
        public PaymentResult charge(String idempotencyKey, ProcessPaymentCommand command) {
            calls.add(idempotencyKey);
            noAnswerIfOut();
            return answers.computeIfAbsent(idempotencyKey, key -> {
                chargesMade++;
                return command.amount().compareTo(new BigDecimal("1000")) > 0
                        ? new PaymentResult(false, null, "over the limit")
                        : new PaymentResult(true, "TXN-" + UUID.randomUUID(), null);
            });
        }

        @Override
        public void refund(String idempotencyKey, UUID orderId, String transactionId) {
            calls.add(idempotencyKey);
            if (refuseRefunds) {
                throw new RefundRejectedException("window closed");
            }
            noAnswerIfOut();
        }

        private void noAnswerIfOut() {
            if (outages > 0) {
                outages--;
                throw new PaymentGatewayUnavailableException("timeout");
            }
        }
    }

    private static final class RecordedReplies implements PaymentReplyPort {
        final List<String> sent = new ArrayList<>();

        @Override
        public void paymentApproved(UUID orderId) {
            sent.add("approved:" + orderId);
        }

        @Override
        public void paymentDeclined(UUID orderId, String reason) {
            sent.add("declined:" + orderId + ":" + reason);
        }
    }

    /** Pass-through, except that it can fail the next transaction before any of its work runs. */
    private static final class FlakyTransactions implements TransactionPort {
        boolean failNext;

        @Override
        public void inTransaction(Runnable work) {
            inTransaction(() -> {
                work.run();
                return null;
            });
        }

        @Override
        public <T> T inTransaction(Supplier<T> work) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("database unavailable");
            }
            return work.get();
        }
    }
}
