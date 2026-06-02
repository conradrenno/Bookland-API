package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;

public interface UpdateOrderStatusUseCase {
    OrderResponse execute(UpdateOrderStatusCommand command);
}
