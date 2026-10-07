package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The batch twin of {@link GetBookByIdUseCase}, for the other services reading books over gRPC: one
 * query however many ids, so a cart of ten books is one round trip, not ten.
 */
public interface GetBooksByIdsUseCase {

    /** The active books among {@code ids}; unknown or removed ones are simply not in the result. */
    List<Book> execute(Collection<UUID> ids);
}
