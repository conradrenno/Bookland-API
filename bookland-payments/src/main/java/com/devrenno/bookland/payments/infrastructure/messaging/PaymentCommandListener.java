package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.RequestChargeUseCase;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.infrastructure.messaging.PaymentMessages.ChargePayment;
import com.devrenno.bookland.payments.infrastructure.messaging.inbox.PaymentsInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Inbound adapter for the charge commands of the checkout saga. It does <b>not</b> call the gateway:
 * it records the charge as PENDING, together with the inbox record, in one transaction, and returns.
 * The gateway worker makes the charge and writes the saga's reply when the gateway answers.
 *
 * <p>That split is what keeps a gateway outage from losing the command. Calling the gateway from
 * here meant a few quick retries by the container and then a skipped message — an order stuck in
 * AWAITING_PAYMENT, possibly with the customer charged.
 *
 * <p>Two guards against a duplicate: the inbox skips a message seen before, and
 * {@code RequestChargeService} records one payment per order — which also covers a second command
 * with a different message id.
 */
@Component
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final RequestChargeUseCase requestChargeUseCase;
    private final PaymentsInbox inbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public PaymentCommandListener(RequestChargeUseCase requestChargeUseCase, PaymentsInbox inbox,
                                  PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.requestChargeUseCase = requestChargeUseCase;
        this.inbox = inbox;
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
            requestChargeUseCase.requestCharge(new ProcessPaymentCommand(
                    command.orderId(), command.customerId(), command.amount(),
                    PaymentMethod.valueOf(command.method())));
        });
    }
}
