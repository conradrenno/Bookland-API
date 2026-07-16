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

        boolean isbnExists = bookPersistencePort.existsByIsbn(command.isbn());
        domainService.validateIsbnUniqueness(command.isbn(), isbnExists);

        Book book = Book.create(
                command.title(),
                ISBN.of(command.isbn()),
                command.authors(),
                command.publisher(),
                command.publicationYear(),
                command.edition(),
                command.synopsis(),
                Price.of(command.price()),
                command.stockQuantity(),
                CategoryId.of(command.categoryId())
        );

        return bookPersistencePort.save(book);
    }
}
