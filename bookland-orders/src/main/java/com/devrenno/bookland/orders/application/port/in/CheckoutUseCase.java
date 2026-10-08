package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.CheckoutCommand;
import com.devrenno.bookland.orders.domain.entity.Order;

public interface CheckoutUseCase {
    Order execute(CheckoutCommand command);
}
