package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;
import com.devrenno.bookland.orders.domain.entity.Cart;

public interface UpdateCartItemUseCase {
    Cart execute(UpdateCartItemCommand command);
}
