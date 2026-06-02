package com.devrenno.bookland.orders.application.port.out;

import java.util.UUID;

public interface RefundPort {
    void refund(UUID orderId);
}
