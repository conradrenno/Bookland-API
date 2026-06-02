package com.devrenno.bookland.wishlist.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WishlistBookInfoAdapter implements WishlistBookInfoPort {

    private final GetBookByIdUseCase getBookByIdUseCase;

    @Override
    public BookResponse getBookInfo(UUID bookId) {
        return getBookByIdUseCase.execute(bookId);
    }
}
