package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;
import com.devrenno.bookland.catalog.application.mapper.CatalogApplicationMapper;
import com.devrenno.bookland.catalog.application.port.in.UpdateBookUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateBookService implements UpdateBookUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;
    private final CatalogApplicationMapper mapper;

    @Override
    public BookResponse execute(UUID bookId, UpdateBookCommand command) {
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
                command.categoryId() != null ? CategoryId.of(command.categoryId()) : null
        );

        return mapper.toResponse(bookPersistencePort.save(book));
    }
}
