package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.domain.entity.Cart;

public interface AddCartItemUseCase {
    Cart execute(AddCartItemCommand command);
}
