package com.devrenno.bookland.reviews.application.port.out;

import java.util.UUID;

/** Throws {@link CatalogUnavailableException} when the catalog cannot answer. */
public interface BookExistsPort {
    boolean exists(UUID bookId);
}
