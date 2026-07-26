package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.CartView;

public interface AddCartItemUseCase {
    CartView execute(AddCartItemCommand command);
}
