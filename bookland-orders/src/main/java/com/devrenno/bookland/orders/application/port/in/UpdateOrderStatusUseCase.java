package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.domain.entity.Order;

public interface UpdateOrderStatusUseCase {
    Order execute(UpdateOrderStatusCommand command);
}
