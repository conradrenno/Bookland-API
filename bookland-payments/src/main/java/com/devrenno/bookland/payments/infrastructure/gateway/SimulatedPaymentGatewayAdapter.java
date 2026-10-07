package com.devrenno.bookland.payments.infrastructure.gateway;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayUnavailableException;
import com.devrenno.bookland.payments.application.port.out.RefundRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for a payment provider, behaving the way a real one does where it matters to us:
 *
 * <ul>
 *   <li>It approves every charge up to {@code bookland.payments.simulated.decline-above} and declines
 *       anything above it, so the checkout's compensation can be triggered on purpose.</li>
 *   <li>It honours the <b>idempotency key</b>: a key seen before gets the first answer back, and no
 *       second charge or refund is made. Only the moves actually made are counted
 *       ({@link #chargesMade()}, {@link #refundsMade()}).</li>
 *   <li>It can be told to fail — no answer for the next few calls, or a refund refused for good — so
 *       the worker's retries can be watched without a real provider going down.</li>
 * </ul>
 *
 * <p>The failures are keyed by customer (charges) and by order (refunds), so a test arranges them
 * before the money moves, for its own data only. Everything lives in memory: a restart forgets the
 * keys, which a real provider keeps for a day or so.
 */
@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    static final String DECLINE_REASON = "Amount above the simulated card limit";

    private final BigDecimal declineAbove;

    private final Map<String, PaymentResult> chargesByKey = new ConcurrentHashMap<>();
    private final Set<String> refundKeys = ConcurrentHashMap.newKeySet();
    private final AtomicInteger chargesMade = new AtomicInteger();
    private final AtomicInteger refundsMade = new AtomicInteger();

    private final Map<UUID, AtomicInteger> chargeOutages = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> refundOutages = new ConcurrentHashMap<>();
    private final Set<UUID> refusedRefunds = ConcurrentHashMap.newKeySet();

    public SimulatedPaymentGatewayAdapter(
            @Value("${bookland.payments.simulated.decline-above:1000.00}") BigDecimal declineAbove) {
        this.declineAbove = declineAbove;
    }

    @Override
    public PaymentResult charge(String idempotencyKey, ProcessPaymentCommand command) {
        failIfOut(chargeOutages, command.customerId(), "charge");
        return chargesByKey.computeIfAbsent(idempotencyKey, key -> {
            chargesMade.incrementAndGet();
            if (command.amount().compareTo(declineAbove) > 0) {
                return new PaymentResult(false, null, DECLINE_REASON);
            }
            return new PaymentResult(true, "SIM-" + UUID.randomUUID(), null);
        });
    }

    @Override
    public void refund(String idempotencyKey, UUID orderId, String transactionId) {
        if (refusedRefunds.contains(orderId)) {
            throw new RefundRejectedException("Simulated refusal: refund window closed for " + transactionId);
        }
        failIfOut(refundOutages, orderId, "refund");
        if (refundKeys.add(idempotencyKey)) {
            refundsMade.incrementAndGet();
        }
    }

    /** The next {@code calls} charges for this customer get no answer. */
    public void failNextCharges(UUID customerId, int calls) {
        chargeOutages.put(customerId, new AtomicInteger(calls));
    }

    /** The next {@code calls} refunds for this order get no answer. */
    public void failNextRefunds(UUID orderId, int calls) {
        refundOutages.put(orderId, new AtomicInteger(calls));
    }

    /** Every refund for this order is refused for good. */
    public void refuseRefunds(UUID orderId) {
        refusedRefunds.add(orderId);
    }

    /** Charges actually made — a repeated key does not count. */
    public int chargesMade() {
        return chargesMade.get();
    }

    /** Refunds actually made — a repeated key does not count. */
    public int refundsMade() {
        return refundsMade.get();
    }

    private static void failIfOut(Map<UUID, AtomicInteger> outages, UUID id, String operation) {
        AtomicInteger remaining = outages.get(id);
        if (remaining != null && remaining.getAndDecrement() > 0) {
            throw new PaymentGatewayUnavailableException("Simulated outage: no answer to the " + operation);
        }
    }
}
