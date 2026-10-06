package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.dto.StockReservationResult;
import com.devrenno.bookland.catalog.application.port.in.ReleaseStockForOrderUseCase;
import com.devrenno.bookland.catalog.application.port.in.ReserveStockForOrderUseCase;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;
import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.dto.StockReservationOutcome;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Calls the catalog in process, for now. In the asynchronous saga this becomes a message to the
 * catalog's command topic; nothing inside the orders module changes when it does.
 */
@Component
@RequiredArgsConstructor
public class StockReservationAdapter implements StockReservationPort {

    private final ReserveStockForOrderUseCase reserveStockForOrderUseCase;
    private final ReleaseStockForOrderUseCase releaseStockForOrderUseCase;

    @Override
    public StockReservationOutcome reserve(UUID orderId, List<StockLine> lines) {
        StockReservationResult result = reserveStockForOrderUseCase.reserve(orderId, lines.stream()
                .map(line -> new StockReservation.Item(line.bookId(), line.quantity()))
                .toList());
        return new StockReservationOutcome(result.reserved(), result.unavailableBookIds());
    }

    @Override
    public void release(UUID orderId) {
        releaseStockForOrderUseCase.release(orderId);
    }
}
