package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;
import com.devrenno.bookland.catalog.application.port.in.UpdateBookUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.Price;

import java.util.UUID;

public class UpdateBookService implements UpdateBookUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;

    private UpdateBookService(BookPersistencePort bookPersistencePort,
                              CategoryPersistencePort categoryPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
        this.categoryPersistencePort = categoryPersistencePort;
    }

    public static UpdateBookService create(BookPersistencePort bookPersistencePort,
                                           CategoryPersistencePort categoryPersistencePort) {
        return new UpdateBookService(bookPersistencePort, categoryPersistencePort);
    }

    @Override
    public Book execute(UUID bookId, UpdateBookCommand command) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (command.categoryId() != null) {
            categoryPersistencePort.findById(command.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        }

        book.update(
                command.title(),
                command.authors(),
                command.publisher(),
                command.publicationYear(),
                command.edition(),
                command.synopsis(),
                command.price() != null ? Price.of(command.price()) : null,
                command.stockQuantity() != null ? command.stockQuantity() : -1,
                command.categoryId() != null ? CategoryId.of(command.categoryId()) : null,
                command.coverImageUrl()
        );

        return bookPersistencePort.save(book);
    }
}
