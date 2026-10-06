package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.PaymentOutcome;
import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.dto.StockReservationOutcome;
import com.devrenno.bookland.orders.application.port.in.CheckoutUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.PaymentDeclinedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CheckoutService implements CheckoutUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final OrderPersistencePort orderPersistencePort;
    private final BookInfoPort bookInfoPort;
    private final StockReservationPort stockReservationPort;
    private final PaymentPort paymentPort;
    private final TransactionPort transactionPort;

    private CheckoutService(CartPersistencePort cartPersistencePort, OrderPersistencePort orderPersistencePort,
                            BookInfoPort bookInfoPort, StockReservationPort stockReservationPort,
                            PaymentPort paymentPort, TransactionPort transactionPort) {
        this.cartPersistencePort = cartPersistencePort;
        this.orderPersistencePort = orderPersistencePort;
        this.bookInfoPort = bookInfoPort;
        this.stockReservationPort = stockReservationPort;
        this.paymentPort = paymentPort;
        this.transactionPort = transactionPort;
    }

    public static CheckoutService create(CartPersistencePort cartPersistencePort,
                                         OrderPersistencePort orderPersistencePort,
                                         BookInfoPort bookInfoPort, StockReservationPort stockReservationPort,
                                         PaymentPort paymentPort, TransactionPort transactionPort) {
        return new CheckoutService(cartPersistencePort, orderPersistencePort, bookInfoPort,
                stockReservationPort, paymentPort, transactionPort);
    }

    /**
     * The checkout saga's steps, in their final order, still run synchronously and in one transaction:
     * reserve the stock, then charge, and give the stock back if the charge is declined. Reserving
     * first is the point — returning units costs nothing, while undoing a charge means a refund — and
     * it removes the old failure where an approved payment was followed by a sold-out decrement.
     *
     * <p>A declined payment must still COMMIT the PAYMENT_FAILED order (and the released reservation),
     * so the declined outcome is returned from the transaction and the exception is thrown only after
     * the commit. A reservation that fails throws inside the transaction, which rolls the whole
     * checkout back: no order is kept, as before.
     */
    @Override
    public Order execute(UUID customerId, PaymentMethod paymentMethod) {
        Outcome outcome = transactionPort.inTransaction(() -> doCheckout(customerId, paymentMethod));
        if (outcome.declineReason != null) {
            throw new PaymentDeclinedException(outcome.declineReason);
        }
        return outcome.order;
    }

    private Outcome doCheckout(UUID customerId, PaymentMethod paymentMethod) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));

        if (cart.getItems().isEmpty()) {
            throw new CartNotFoundException(customerId);
        }

        List<UUID> unavailable = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            // A courtesy check, not the guarantee: it lets an obviously unfulfillable cart fail
            // before anything is reserved or charged. The binding check is the reservation below,
            // which the catalog evaluates atomically. A book removed from the catalog since it was
            // added counts as unavailable, not as a missing resource.
            Optional<BookInfo> found = bookInfoPort.findBookInfo(item.getBookId());
            if (found.isEmpty() || found.get().stockQuantity() < item.getQuantity()) {
                unavailable.add(item.getBookId());
            } else {
                BookInfo book = found.get();
                orderItems.add(OrderItem.of(
                        book.id(), book.title(), book.coverImageUrl(),
                        item.getQuantity(), item.getUnitPriceAtAddition()));
            }
        }

        if (!unavailable.isEmpty()) {
            throw new CartItemUnavailableException(unavailable);
        }

        Order order = Order.fromCart(customerId, orderItems);

        // Step 1: reserve. All lines or none — a checkout that lost the race for the last copies
        // ends here, before the customer is charged.
        StockReservationOutcome reservation = stockReservationPort.reserve(order.getId(), order.getItems().stream()
                .map(item -> new StockLine(item.getBookId(), item.getQuantity()))
                .toList());
        if (!reservation.reserved()) {
            throw new CartItemUnavailableException(reservation.unavailableBookIds());
        }
        order.transitionStatus(OrderStatus.AWAITING_PAYMENT, customerId);

        // Step 2: charge.
        PaymentOutcome payment = paymentPort.charge(
                order.getId(), customerId, order.getTotalAmount(), paymentMethod);

        if (payment.approved()) {
            order.transitionStatus(OrderStatus.CONFIRMED, customerId);
            Order saved = orderPersistencePort.save(order);
            cartPersistencePort.deleteByCustomerId(customerId);
            return Outcome.approved(saved);
        }

        // Compensation for step 1: the units go back to the catalog. The reason is never left null:
        // a null here would read as an approval to execute(), which tells the two apart by it.
        String reason = Objects.requireNonNullElse(payment.declineReason(), "Payment declined");
        stockReservationPort.release(order.getId());
        order.transitionStatus(OrderStatus.PAYMENT_FAILED, customerId, reason);
        orderPersistencePort.save(order);
        return Outcome.declined(reason);
    }

    private record Outcome(Order order, String declineReason) {
        static Outcome approved(Order order) {
            return new Outcome(order, null);
        }

        static Outcome declined(String reason) {
            return new Outcome(null, reason);
        }
    }
}
