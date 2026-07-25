package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.UploadBookCoverCommand;
import com.devrenno.bookland.catalog.application.port.in.UploadBookCoverUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.ImageStoragePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.InvalidImageException;

import java.util.Set;
import java.util.UUID;

public class UploadBookCoverService implements UploadBookCoverUseCase {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final BookPersistencePort bookPersistencePort;
    private final ImageStoragePort imageStoragePort;

    private UploadBookCoverService(BookPersistencePort bookPersistencePort,
                                   ImageStoragePort imageStoragePort) {
        this.bookPersistencePort = bookPersistencePort;
        this.imageStoragePort = imageStoragePort;
    }

    public static UploadBookCoverService create(BookPersistencePort bookPersistencePort,
                                                ImageStoragePort imageStoragePort) {
        return new UploadBookCoverService(bookPersistencePort, imageStoragePort);
    }

    @Override
    public Book execute(UUID bookId, UploadBookCoverCommand command) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (command.content() == null || command.content().length == 0) {
            throw new InvalidImageException("Cover image file is required and must not be empty");
        }
        String contentType = command.contentType() == null ? "" : command.contentType().toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageException("Unsupported image type '" + command.contentType()
                    + "'. Allowed: JPEG, PNG, WEBP");
        }

        String url = imageStoragePort.store(command.content(), command.originalFilename(), contentType);
        book.updateCoverImage(url);
        return bookPersistencePort.save(book);
    }
}
