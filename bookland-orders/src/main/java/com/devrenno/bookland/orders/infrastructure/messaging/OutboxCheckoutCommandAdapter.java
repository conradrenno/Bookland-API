package com.devrenno.bookland.orders.infrastructure.messaging;

import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.port.out.CheckoutCommandPort;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.ChargePayment;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.Line;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.ReleaseStock;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.ReserveStock;
import com.devrenno.bookland.orders.infrastructure.messaging.outbox.OrdersOutbox;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * "Sending" a saga command means writing it to the orders outbox, in the caller's transaction — the
 * one that moved the order to the step requiring it. The relay delivers it afterwards, keyed by the
 * order id so the commands of one order stay in order.
 */
@Component
public class OutboxCheckoutCommandAdapter implements CheckoutCommandPort {

    private final OrdersOutbox outbox;

    public OutboxCheckoutCommandAdapter(OrdersOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void requestStockReservation(UUID orderId, List<StockLine> lines) {
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, orderId, OrdersKafkaConfig.RESERVE_STOCK, new ReserveStock(
                messageId, OrdersKafkaConfig.RESERVE_STOCK, orderId,
                lines.stream().map(line -> new Line(line.bookId(), line.quantity())).toList()));
    }

    @Override
    public void requestStockRelease(UUID orderId) {
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, orderId, OrdersKafkaConfig.RELEASE_STOCK,
                new ReleaseStock(messageId, OrdersKafkaConfig.RELEASE_STOCK, orderId));
    }

    @Override
    public void requestPayment(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method) {
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, orderId, OrdersKafkaConfig.CHARGE_PAYMENT, new ChargePayment(
                messageId, OrdersKafkaConfig.CHARGE_PAYMENT, orderId, customerId, amount, method.name()));
    }
}
