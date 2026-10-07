package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.application.port.out.PaymentReplyPort;
import com.devrenno.bookland.payments.infrastructure.messaging.PaymentMessages.PaymentReply;
import com.devrenno.bookland.payments.infrastructure.messaging.outbox.PaymentsOutbox;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Writes the saga's reply to a charge into the payments outbox, in the transaction that records the
 * gateway's answer. The relay sends it on {@link PaymentsKafkaConfig#PAYMENT_REPLIES_TOPIC}.
 */
@Component
public class OutboxPaymentReplyAdapter implements PaymentReplyPort {

    private final PaymentsOutbox outbox;

    public OutboxPaymentReplyAdapter(PaymentsOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void paymentApproved(UUID orderId) {
        reply(orderId, PaymentsKafkaConfig.PAYMENT_APPROVED, null);
    }

    @Override
    public void paymentDeclined(UUID orderId, String reason) {
        reply(orderId, PaymentsKafkaConfig.PAYMENT_DECLINED, reason);
    }

    private void reply(UUID orderId, String type, String reason) {
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, orderId, type, new PaymentReply(messageId, type, orderId, reason));
    }
}
