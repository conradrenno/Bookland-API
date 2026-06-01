package com.devrenno.bookland.catalog.domain.service;

import com.devrenno.bookland.catalog.domain.exception.IsbnAlreadyExistsException;

public class CatalogDomainService {

    public void validateIsbnUniqueness(String isbn, boolean alreadyExists) {
        if (alreadyExists) {
            throw new IsbnAlreadyExistsException(isbn);
        }
    }
}
