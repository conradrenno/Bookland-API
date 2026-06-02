package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.CartResponse;

public interface AddCartItemUseCase {
    CartResponse execute(AddCartItemCommand command);
}
