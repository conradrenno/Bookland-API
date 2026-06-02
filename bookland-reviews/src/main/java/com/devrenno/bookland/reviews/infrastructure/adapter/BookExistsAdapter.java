package com.devrenno.bookland.reviews.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookExistsAdapter implements BookExistsPort {

    private final GetBookByIdUseCase getBookByIdUseCase;

    @Override
    public boolean exists(UUID bookId) {
        try {
            getBookByIdUseCase.execute(bookId);
            return true;
        } catch (BookNotFoundException e) {
            return false;
        }
    }
}
