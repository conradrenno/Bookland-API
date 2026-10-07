package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The commands the checkout saga sends to the other services. Each one is a request, not a call: it
 * returns nothing, and the answer arrives later as a reply ({@code CheckoutSagaUseCase}).
 *
 * <p>Implemented by writing to the orders outbox in the caller's transaction, so a command is sent
 * if and only if the change of state that requires it commits.
 */
public interface CheckoutCommandPort {

    void requestStockReservation(UUID orderId, List<StockLine> lines);

    /** The compensation of a reservation, when the payment that should follow it is declined. */
    void requestStockRelease(UUID orderId);

    void requestPayment(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method);
}
