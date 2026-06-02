package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.port.in.CancelOrderUseCase;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final BookStockPort bookStockPort;

    @Override
    @Transactional
    public OrderResponse execute(UUID orderId, UUID customerId) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel(customerId);
        for (var item : order.getItems()) {
            bookStockPort.adjustStock(item.getBookId(), item.getQuantity());
        }
        return OrderResponseMapper.toOrderResponse(orderPersistencePort.save(order));
    }
}
