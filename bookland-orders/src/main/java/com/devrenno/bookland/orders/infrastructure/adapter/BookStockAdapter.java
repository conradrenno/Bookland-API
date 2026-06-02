package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.AdjustBookStockUseCase;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookStockAdapter implements BookStockPort {

    private final AdjustBookStockUseCase adjustBookStockUseCase;

    @Override
    public void adjustStock(UUID bookId, int delta) {
        adjustBookStockUseCase.adjustStock(bookId, delta);
    }
}
