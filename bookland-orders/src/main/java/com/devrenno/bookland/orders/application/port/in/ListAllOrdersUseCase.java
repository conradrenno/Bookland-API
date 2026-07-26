package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

/** Admin-facing listing of every order, optionally filtered by status. */
public interface ListAllOrdersUseCase {
    PageResult<Order> execute(OrderStatus status, PageQuery pageQuery);
}
