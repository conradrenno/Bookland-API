package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderPersistencePort {
    Order save(Order order);
    Optional<Order> findById(UUID orderId);
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
}
