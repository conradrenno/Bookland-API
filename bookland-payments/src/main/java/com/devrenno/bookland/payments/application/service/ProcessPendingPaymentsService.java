package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.dto.RetryPolicy;
import com.devrenno.bookland.payments.application.port.in.ProcessPendingPaymentsUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.application.port.out.PaymentReplyPort;
import com.devrenno.bookland.payments.application.port.out.RefundRejectedException;
import com.devrenno.bookland.payments.application.port.out.TransactionPort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Takes pending charges and refunds to the gateway. The call happens <em>outside</em> any database
 * transaction, and its answer is recorded in one afterwards. If the process dies between the two, the
 * payment is still pending and is called again, with the same idempotency key, so the gateway answers
 * as before instead of moving the money twice.
 *
 * <p>A call that gets no answer, for whatever reason, is not an answer: the payment stays pending and
 * is retried later ({@link RetryPolicy}). Only the gateway's own "no" settles it.
 *
 * <p>One instance only, like the outbox relays: two workers would call the gateway for the same
 * payment at once. The idempotency key would still keep the money right, but the answers would race
 * to be recorded.
 */
public class ProcessPendingPaymentsService implements ProcessPendingPaymentsUseCase {

    private final PaymentPersistencePort persistence;
    private final PaymentGatewayPort gateway;
    private final PaymentReplyPort replyPort;
    private final TransactionPort transactionPort;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    private ProcessPendingPaymentsService(PaymentPersistencePort persistence, PaymentGatewayPort gateway,
                                          PaymentReplyPort replyPort, TransactionPort transactionPort,
                                          RetryPolicy retryPolicy, Clock clock) {
        this.persistence = persistence;
        this.gateway = gateway;
        this.replyPort = replyPort;
        this.transactionPort = transactionPort;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    public static ProcessPendingPaymentsService create(PaymentPersistencePort persistence, PaymentGatewayPort gateway,
                                                       PaymentReplyPort replyPort, TransactionPort transactionPort,
                                                       RetryPolicy retryPolicy, Clock clock) {
        return new ProcessPendingPaymentsService(persistence, gateway, replyPort, transactionPort, retryPolicy, clock);
    }

    /** One charge and at most one refund per order, so the order id names each operation. */
    static String chargeKey(UUID orderId) {
        return "charge:" + orderId;
    }

    static String refundKey(UUID orderId) {
        return "refund:" + orderId;
    }

    @Override
    public List<UUID> findDue(int limit) {
        return persistence.findDue(clock.instant(), limit).stream().map(Payment::getId).toList();
    }

    @Override
    public void process(UUID paymentId) {
        Payment payment = persistence.findById(paymentId).orElse(null);
        if (payment == null || !payment.awaitsGateway()) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            charge(payment);
        } else {
            refund(payment);
        }
    }

    private void charge(Payment payment) {
        PaymentResult result;
        try {
            result = gateway.charge(chargeKey(payment.getOrderId()), new ProcessPaymentCommand(
                    payment.getOrderId(), payment.getCustomerId(), payment.getAmount(), payment.getMethod()));
        } catch (RuntimeException noAnswer) {
            retryLater(payment, noAnswer);
            return;
        }
        transactionPort.inTransaction(() -> {
            Instant now = clock.instant();
            if (result.approved()) {
                payment.approve(result.transactionId(), now);
                persistence.save(payment);
                replyPort.paymentApproved(payment.getOrderId());
            } else {
                payment.decline(result.declineReason(), now);
                persistence.save(payment);
                replyPort.paymentDeclined(payment.getOrderId(), result.declineReason());
            }
        });
    }

    private void refund(Payment payment) {
        try {
            gateway.refund(refundKey(payment.getOrderId()), payment.getOrderId(), payment.getGatewayTransactionId());
        } catch (RefundRejectedException refused) {
            transactionPort.inTransaction(() -> {
                payment.refundRejected(refused.getMessage(), clock.instant());
                persistence.save(payment);
            });
            return;
        } catch (RuntimeException noAnswer) {
            retryLater(payment, noAnswer);
            return;
        }
        transactionPort.inTransaction(() -> {
            payment.markRefunded(clock.instant());
            persistence.save(payment);
        });
    }

    /**
     * Any exception from the gateway call counts as "no answer", not only
     * {@code PaymentGatewayUnavailableException}: an outcome nobody read is unknown, and the safe
     * reading of unknown is "try again with the same key".
     */
    private void retryLater(Payment payment, RuntimeException noAnswer) {
        transactionPort.inTransaction(() -> {
            Instant now = clock.instant();
            Instant retryAt = now.plus(retryPolicy.delayAfter(payment.getAttempts() + 1));
            payment.gatewayCallFailed(noAnswer.getClass().getSimpleName() + ": " + noAnswer.getMessage(),
                    retryAt, now);
            persistence.save(payment);
        });
    }
}
