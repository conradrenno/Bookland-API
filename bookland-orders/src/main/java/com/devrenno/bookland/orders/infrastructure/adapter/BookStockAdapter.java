package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.DecrementBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.in.IncrementBookStockUseCase;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookStockAdapter implements BookStockPort {

    private final DecrementBookStockUseCase decrementBookStockUseCase;
    private final IncrementBookStockUseCase incrementBookStockUseCase;

    @Override
    public boolean tryDecrementStock(UUID bookId, int quantity) {
        return decrementBookStockUseCase.tryDecrement(bookId, quantity);
    }

    @Override
    public void incrementStock(UUID bookId, int quantity) {
        incrementBookStockUseCase.increment(bookId, quantity);
    }
}
