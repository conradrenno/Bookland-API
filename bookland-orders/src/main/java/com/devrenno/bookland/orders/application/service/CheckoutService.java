package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CheckoutCommand;
import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.port.in.CheckoutUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.CheckoutCommandPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartEmptyException;
import com.devrenno.bookland.orders.domain.exception.CheckoutInProgressException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Starts the checkout saga and returns at once, with the order PENDING. The rest — reserving the
 * stock, charging, giving the stock back on a decline — happens as the replies arrive
 * ({@link CheckoutSagaService}).
 */
public class CheckoutService implements CheckoutUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final OrderPersistencePort orderPersistencePort;
    private final BookInfoPort bookInfoPort;
    private final CheckoutCommandPort checkoutCommandPort;
    private final TransactionPort transactionPort;

    private CheckoutService(CartPersistencePort cartPersistencePort, OrderPersistencePort orderPersistencePort,
                            BookInfoPort bookInfoPort, CheckoutCommandPort checkoutCommandPort,
                            TransactionPort transactionPort) {
        this.cartPersistencePort = cartPersistencePort;
        this.orderPersistencePort = orderPersistencePort;
        this.bookInfoPort = bookInfoPort;
        this.checkoutCommandPort = checkoutCommandPort;
        this.transactionPort = transactionPort;
    }

    public static CheckoutService create(CartPersistencePort cartPersistencePort,
                                         OrderPersistencePort orderPersistencePort,
                                         BookInfoPort bookInfoPort, CheckoutCommandPort checkoutCommandPort,
                                         TransactionPort transactionPort) {
        return new CheckoutService(cartPersistencePort, orderPersistencePort, bookInfoPort,
                checkoutCommandPort, transactionPort);
    }

    /**
     * One transaction holds the claim on the cart, the PENDING order and the first command in the
     * outbox: the saga starts entirely or not at all. A cart already claimed by an unfinished checkout
     * is refused — two sagas for one cart would both charge the customer.
     */
    @Override
    public Order execute(CheckoutCommand command) {
        UUID customerId = command.customerId();
        return transactionPort.inTransaction(() -> {
            Cart cart = cartPersistencePort.findByCustomerId(customerId)
                    .orElseThrow(() -> new CartEmptyException(customerId));
            if (cart.getItems().isEmpty()) {
                throw new CartEmptyException(customerId);
            }

            Order order = Order.fromCart(customerId, command.customerEmail(), command.customerName(),
                    orderItemsOf(cart), command.paymentMethod());

            if (!cartPersistencePort.claimForCheckout(customerId, order.getId())) {
                throw new CheckoutInProgressException(customerId);
            }

            Order saved = orderPersistencePort.save(order);
            checkoutCommandPort.requestStockReservation(saved.getId(), saved.getItems().stream()
                    .map(item -> new StockLine(item.getBookId(), item.getQuantity()))
                    .toList());
            return saved;
        });
    }

    /**
     * A courtesy check, not the guarantee: it lets an obviously unfulfillable cart fail with a 409 at
     * once instead of as a REJECTED order a moment later. The binding check is the catalog's
     * reservation, evaluated atomically when the command arrives. A book removed from the catalog
     * since it was added counts as unavailable. A catalog that cannot answer stops the checkout
     * ({@code CatalogUnavailableException}, 503): the order snapshots titles and covers from here.
     */
    private List<OrderItem> orderItemsOf(Cart cart) {
        List<UUID> unavailable = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        Map<UUID, BookInfo> books = bookInfoPort.findBookInfos(
                cart.getItems().stream().map(CartItem::getBookId).toList());
        for (CartItem item : cart.getItems()) {
            BookInfo book = books.get(item.getBookId());
            if (book == null || book.stockQuantity() < item.getQuantity()) {
                unavailable.add(item.getBookId());
            } else {
                orderItems.add(OrderItem.of(book.id(), book.title(), book.coverImageUrl(),
                        item.getQuantity(), item.getUnitPriceAtAddition()));
            }
        }
        if (!unavailable.isEmpty()) {
            throw new CartItemUnavailableException(unavailable);
        }
        return orderItems;
    }
}
