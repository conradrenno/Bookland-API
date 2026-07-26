package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.port.in.CreateBookUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import com.devrenno.bookland.catalog.domain.service.CatalogDomainService;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;

public class CreateBookService implements CreateBookUseCase {

    private final CatalogDomainService domainService;
    private final BookPersistencePort bookPersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;

    private CreateBookService(CatalogDomainService domainService,
                              BookPersistencePort bookPersistencePort,
                              CategoryPersistencePort categoryPersistencePort) {
        this.domainService = domainService;
        this.bookPersistencePort = bookPersistencePort;
        this.categoryPersistencePort = categoryPersistencePort;
    }

    public static CreateBookService create(CatalogDomainService domainService,
                                           BookPersistencePort bookPersistencePort,
                                           CategoryPersistencePort categoryPersistencePort) {
        return new CreateBookService(domainService, bookPersistencePort, categoryPersistencePort);
    }

    @Override
    public Book execute(CreateBookCommand command) {
        categoryPersistencePort.findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        // Uniqueness is checked on the canonical form, so "978-0132350884" cannot slip past a
        // stored "9780132350884".
        ISBN isbn = ISBN.of(command.isbn());
        boolean isbnExists = bookPersistencePort.existsByIsbn(isbn.value());
        domainService.validateIsbnUniqueness(isbn.value(), isbnExists);

        Book book = Book.create(
                command.title(),
                isbn,
                command.authors(),
                command.publisher(),
                command.publicationYear(),
                command.edition(),
                command.synopsis(),
                Price.of(command.price()),
                command.stockQuantity(),
                CategoryId.of(command.categoryId()),
                command.coverImageUrl()
        );

        return bookPersistencePort.save(book);
    }
}
