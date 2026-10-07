package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.ProcessPaymentUseCase;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.infrastructure.messaging.PaymentMessages.ChargePayment;
import com.devrenno.bookland.payments.infrastructure.messaging.PaymentMessages.PaymentReply;
import com.devrenno.bookland.payments.infrastructure.messaging.inbox.PaymentsInbox;
import com.devrenno.bookland.payments.infrastructure.messaging.outbox.PaymentsOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Inbound adapter for the charge commands of the checkout saga: the inbox record, the payment and the
 * reply in the outbox commit together, in one transaction.
 *
 * <p>The charge is the one step of the saga that cannot be taken back by writing to our own
 * database, which is why it has two guards against a duplicate: the inbox skips a message seen
 * before, and {@code ProcessPaymentService} answers an order already charged from the stored payment
 * without reaching the gateway — the latter also covers a second command with a different message id.
 */
@Component
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final PaymentsInbox inbox;
    private final PaymentsOutbox outbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public PaymentCommandListener(ProcessPaymentUseCase processPaymentUseCase, PaymentsInbox inbox,
                                  PaymentsOutbox outbox, PlatformTransactionManager transactionManager,
                                  JsonMapper jsonMapper) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.inbox = inbox;
        this.outbox = outbox;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = PaymentsKafkaConfig.PAYMENT_COMMANDS_TOPIC, groupId = "bookland-payments",
            containerFactory = PaymentsKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void on(String payload) {
        ChargePayment command = jsonMapper.readValue(payload, ChargePayment.class);
        if (!PaymentsKafkaConfig.CHARGE_PAYMENT.equals(command.type())) {
            log.warn("Unknown payment command type {}, ignored", command.type());
            return;
        }
        transactionTemplate.executeWithoutResult(tx -> {
            if (!inbox.firstDelivery(command.messageId())) {
                log.info("Charge for order {} already handled, skipped", command.orderId());
                return;
            }
            PaymentResult result = processPaymentUseCase.processPayment(new ProcessPaymentCommand(
                    command.orderId(), command.customerId(), command.amount(),
                    PaymentMethod.valueOf(command.method())));
            String type = result.approved() ? PaymentsKafkaConfig.PAYMENT_APPROVED : PaymentsKafkaConfig.PAYMENT_DECLINED;
            UUID messageId = UUID.randomUUID();
            outbox.append(messageId, command.orderId(), type,
                    new PaymentReply(messageId, type, command.orderId(), result.declineReason()));
        });
    }
}
