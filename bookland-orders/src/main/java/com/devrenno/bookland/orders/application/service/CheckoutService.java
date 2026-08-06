package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.port.in.CheckoutUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.PaymentDeclinedException;
import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CheckoutService implements CheckoutUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final OrderPersistencePort orderPersistencePort;
    private final BookInfoPort bookInfoPort;
    private final BookStockPort bookStockPort;
    private final PaymentPort paymentPort;
    private final TransactionPort transactionPort;

    private CheckoutService(CartPersistencePort cartPersistencePort, OrderPersistencePort orderPersistencePort,
                            BookInfoPort bookInfoPort, BookStockPort bookStockPort,
                            PaymentPort paymentPort, TransactionPort transactionPort) {
        this.cartPersistencePort = cartPersistencePort;
        this.orderPersistencePort = orderPersistencePort;
        this.bookInfoPort = bookInfoPort;
        this.bookStockPort = bookStockPort;
        this.paymentPort = paymentPort;
        this.transactionPort = transactionPort;
    }

    public static CheckoutService create(CartPersistencePort cartPersistencePort,
                                         OrderPersistencePort orderPersistencePort,
                                         BookInfoPort bookInfoPort, BookStockPort bookStockPort,
                                         PaymentPort paymentPort, TransactionPort transactionPort) {
        return new CheckoutService(cartPersistencePort, orderPersistencePort, bookInfoPort,
                bookStockPort, paymentPort, transactionPort);
    }

    /**
     * The whole checkout runs in a single transaction. A declined payment must still COMMIT the
     * PAYMENT_FAILED order (the old @Transactional(noRollbackFor = PaymentDeclinedException.class)
     * semantics), so the declined outcome is returned from the transaction and the exception is
     * thrown only after the commit. Any other exception rolls the transaction back as before —
     * including a CartItemUnavailableException raised after an approved payment, when a concurrent
     * checkout took the last copies. That rollback discards the payment record along with the order,
     * which is correct against the simulated gateway but would need a compensating refund against a
     * real one; see the stock-reservation note in the README roadmap.
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
            // This pass is a courtesy check, not the guarantee: it lets an obviously unfulfillable
            // cart fail before the customer is charged. The binding check is the conditional
            // decrement below, which is the only one the store evaluates atomically.
            // A book removed from the catalog since it was added counts as unavailable, not as a
            // missing resource — the customer gets the same "remove these items" outcome as an
            // out-of-stock one.
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

        PaymentResult result = paymentPort.processPayment(
                order.getId(), customerId, order.getTotalAmount(), paymentMethod);

        if (result.approved()) {
            // Each decrement is guarded by the store, so a checkout that lost the race for the last
            // copies fails here instead of confirming an order the catalog cannot fulfil. Throwing
            // rolls the transaction back — the payment row and any decrement already applied in this
            // loop go with it, which is what keeps a partially-fulfilled order from existing.
            List<UUID> soldOut = new ArrayList<>();
            for (OrderItem item : orderItems) {
                if (!bookStockPort.tryDecrementStock(item.getBookId(), item.getQuantity())) {
                    soldOut.add(item.getBookId());
                }
            }
            if (!soldOut.isEmpty()) {
                throw new CartItemUnavailableException(soldOut);
            }
            order.transitionStatus(OrderStatus.CONFIRMED, customerId);
            Order saved = orderPersistencePort.save(order);
            cartPersistencePort.deleteByCustomerId(customerId);
            return Outcome.approved(saved);
        } else {
            order.transitionStatus(OrderStatus.PAYMENT_FAILED, customerId);
            orderPersistencePort.save(order);
            return Outcome.declined(result.declineReason());
        }
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
