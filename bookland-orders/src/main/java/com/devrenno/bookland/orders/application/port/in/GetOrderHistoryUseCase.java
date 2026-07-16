package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.domain.entity.Order;

import java.util.UUID;

public interface GetOrderHistoryUseCase {
    PageResult<Order> execute(UUID customerId, PageQuery pageQuery);
}
