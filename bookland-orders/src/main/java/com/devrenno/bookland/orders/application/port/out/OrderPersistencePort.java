package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.domain.entity.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderPersistencePort {
    Order save(Order order);
    Optional<Order> findById(UUID orderId);
    PageResult<Order> findByCustomerId(UUID customerId, PageQuery pageQuery);
}
