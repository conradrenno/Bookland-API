package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.CheckoutSagaUseCase;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.CheckoutCommandPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The orchestrator of the checkout saga. The order's status is the saga's state:
 *
 * <pre>
 * PENDING ──StockReserved──────────► AWAITING_PAYMENT ──PaymentApproved──► CONFIRMED (cart emptied)
 *    │                                     │
 *    └─StockReservationFailed─► REJECTED   └─PaymentDeclined─► PAYMENT_FAILED + ReleaseStock (compensation)
 * </pre>
 *
 * <p>Each reply is accepted only in the status that expects it; anything else is a duplicate or a
 * reply to a step already past, and is ignored. A failed order releases the claim on the cart, so the
 * customer can try again with the cart as it was.
 *
 * <p>Transitions made here are recorded with no {@code changedBy}: the saga moved the order, not a
 * person.
 */
public class CheckoutSagaService implements CheckoutSagaUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final CartPersistencePort cartPersistencePort;
    private final CheckoutCommandPort checkoutCommandPort;
    private final TransactionPort transactionPort;

    private CheckoutSagaService(OrderPersistencePort orderPersistencePort, CartPersistencePort cartPersistencePort,
                                CheckoutCommandPort checkoutCommandPort, TransactionPort transactionPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.cartPersistencePort = cartPersistencePort;
        this.checkoutCommandPort = checkoutCommandPort;
        this.transactionPort = transactionPort;
    }

    public static CheckoutSagaService create(OrderPersistencePort orderPersistencePort,
                                             CartPersistencePort cartPersistencePort,
                                             CheckoutCommandPort checkoutCommandPort,
                                             TransactionPort transactionPort) {
        return new CheckoutSagaService(orderPersistencePort, cartPersistencePort, checkoutCommandPort,
                transactionPort);
    }

    @Override
    public boolean onStockReserved(UUID orderId) {
        return advance(orderId, OrderStatus.PENDING, order -> {
            order.transitionStatus(OrderStatus.AWAITING_PAYMENT, null);
            orderPersistencePort.save(order);
            checkoutCommandPort.requestPayment(order.getId(), order.getCustomerId(),
                    order.getTotalAmount(), order.getPaymentMethod());
        });
    }

    @Override
    public boolean onStockReservationFailed(UUID orderId, List<UUID> unavailableBookIds) {
        return advance(orderId, OrderStatus.PENDING, order -> {
            String reason = unavailableBookIds.isEmpty()
                    ? "Some items are no longer available"
                    : "Unavailable books: " + unavailableBookIds.stream().map(UUID::toString)
                    .collect(Collectors.joining(", "));
            order.transitionStatus(OrderStatus.REJECTED, null, reason);
            orderPersistencePort.save(order);
            cartPersistencePort.releaseCheckoutClaim(order.getCustomerId());
        });
    }

    @Override
    public boolean onPaymentApproved(UUID orderId) {
        return advance(orderId, OrderStatus.AWAITING_PAYMENT, order -> {
            order.transitionStatus(OrderStatus.CONFIRMED, null);
            orderPersistencePort.save(order);
            cartPersistencePort.deleteByCustomerId(order.getCustomerId());
        });
    }

    @Override
    public boolean onPaymentDeclined(UUID orderId, String reason) {
        return advance(orderId, OrderStatus.AWAITING_PAYMENT, order -> {
            order.transitionStatus(OrderStatus.PAYMENT_FAILED, null,
                    reason != null ? reason : "Payment declined");
            orderPersistencePort.save(order);
            checkoutCommandPort.requestStockRelease(order.getId());
            cartPersistencePort.releaseCheckoutClaim(order.getCustomerId());
        });
    }

    /** Applies the step only when the order exists and is in the status the reply belongs to. */
    private boolean advance(UUID orderId, OrderStatus expected, Consumer<Order> step) {
        return transactionPort.inTransaction(() -> {
            Optional<Order> found = orderPersistencePort.findById(orderId);
            if (found.isEmpty() || found.get().getStatus() != expected) {
                return false;
            }
            step.accept(found.get());
            return true;
        });
    }
}
