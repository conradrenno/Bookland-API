package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;

public interface UpdateCartItemUseCase {
    CartResponse execute(UpdateCartItemCommand command);
}
