package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.port.in.CheckoutUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class CheckoutService implements CheckoutUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final OrderPersistencePort orderPersistencePort;
    private final BookInfoPort bookInfoPort;
    private final BookStockPort bookStockPort;

    @Override
    @Transactional
    public OrderResponse execute(UUID customerId) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));

        if (cart.getItems().isEmpty()) {
            throw new CartNotFoundException(customerId);
        }

        List<UUID> unavailable = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            BookInfo book = bookInfoPort.getBookInfo(item.getBookId());
            if (book.stockQuantity() < item.getQuantity()) {
                unavailable.add(item.getBookId());
            } else {
                orderItems.add(OrderItem.builder()
                        .bookId(book.id())
                        .title(book.title())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPriceAtAddition())
                        .build());
            }
        }

        if (!unavailable.isEmpty()) {
            throw new CartItemUnavailableException(unavailable);
        }

        Order order = Order.fromCart(customerId, orderItems);
        for (OrderItem item : orderItems) {
            bookStockPort.adjustStock(item.getBookId(), -item.getQuantity());
        }
        Order saved = orderPersistencePort.save(order);
        cartPersistencePort.deleteByCustomerId(customerId);

        return OrderResponseMapper.toOrderResponse(saved);
    }
}
