package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.ReleaseStockForOrderUseCase;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Releases on cancellation by calling the catalog in process. Reserving, and the release that
 * compensates a declined payment, already travel as messages ({@code OutboxCheckoutCommandAdapter});
 * this last direct call goes when the cancellation becomes an event.
 */
@Component
@RequiredArgsConstructor
public class StockReservationAdapter implements StockReservationPort {

    private final ReleaseStockForOrderUseCase releaseStockForOrderUseCase;

    @Override
    public void release(UUID orderId) {
        releaseStockForOrderUseCase.release(orderId);
    }
}
