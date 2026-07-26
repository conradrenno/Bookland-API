package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;

public interface UpdateCartItemUseCase {
    CartView execute(UpdateCartItemCommand command);
}
