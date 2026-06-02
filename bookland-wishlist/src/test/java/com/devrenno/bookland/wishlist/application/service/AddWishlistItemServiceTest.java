package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.wishlist.application.dto.WishlistResponse;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddWishlistItemServiceTest {

    @Mock private WishlistPersistencePort wishlistPersistencePort;
    @Mock private WishlistBookInfoPort wishlistBookInfoPort;
    @InjectMocks private AddWishlistItemService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @Test
    void execute_shouldAddItemToEmptyWishlist() {
        BookResponse book = buildBookResponse();
        Wishlist savedWishlist = Wishlist.createFor(customerId);

        when(wishlistBookInfoPort.getBookInfo(any())).thenReturn(book);
        when(wishlistPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(wishlistPersistencePort.save(any())).thenReturn(savedWishlist);

        WishlistResponse response = service.execute(customerId, bookId);

        assertThat(response).isNotNull();
        verify(wishlistPersistencePort).save(any());
    }

    @Test
    void execute_shouldThrowAlreadyExists_whenItemAlreadyInWishlist() {
        BookResponse book = buildBookResponse();
        Wishlist wishlist = Wishlist.createFor(customerId);
        wishlist.addItem(bookId);

        when(wishlistBookInfoPort.getBookInfo(bookId)).thenReturn(book);
        when(wishlistPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(wishlist));

        assertThatThrownBy(() -> service.execute(customerId, bookId))
                .isInstanceOf(WishlistItemAlreadyExistsException.class);

        verify(wishlistPersistencePort, never()).save(any());
    }

    private BookResponse buildBookResponse() {
        return new BookResponse(bookId, "Clean Code", "978-0132350884", List.of("Robert Martin"),
                "Prentice Hall", 2008, "1st", "Great book", BigDecimal.valueOf(49.90),
                10, true, UUID.randomUUID(), 4.8);
    }
}
