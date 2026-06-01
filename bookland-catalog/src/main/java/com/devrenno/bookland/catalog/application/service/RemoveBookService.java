package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.port.in.RemoveBookUseCase;
import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookHasActiveOrdersException;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RemoveBookService implements RemoveBookUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final ActiveOrderCheckPort activeOrderCheckPort;

    @Override
    public void execute(UUID bookId) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (activeOrderCheckPort.hasActiveOrdersForBook(bookId)) {
            throw new BookHasActiveOrdersException(bookId);
        }

        book.deactivate();
        bookPersistencePort.save(book);
    }
}
