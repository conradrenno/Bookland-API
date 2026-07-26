package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.dto.WishlistBookInfo;
import com.devrenno.bookland.wishlist.application.dto.WishlistView;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private AddWishlistItemService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = AddWishlistItemService.create(wishlistPersistencePort, wishlistBookInfoPort);
    }

    @Test
    void execute_shouldAddItemToEmptyWishlist() {
        WishlistBookInfo book = buildBookInfo();
        Wishlist savedWishlist = Wishlist.createFor(customerId);

        when(wishlistBookInfoPort.getBookInfo(any())).thenReturn(book);
        when(wishlistPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(wishlistPersistencePort.save(any())).thenReturn(savedWishlist);

        WishlistView view = service.execute(customerId, bookId);

        assertThat(view).isNotNull();
        assertThat(view.customerId()).isEqualTo(customerId);
        verify(wishlistPersistencePort).save(any());
    }

    @Test
    void execute_shouldThrowAlreadyExists_whenItemAlreadyInWishlist() {
        WishlistBookInfo book = buildBookInfo();
        Wishlist wishlist = Wishlist.createFor(customerId);
        wishlist.addItem(bookId);

        when(wishlistBookInfoPort.getBookInfo(bookId)).thenReturn(book);
        when(wishlistPersistencePort.findByCustomerId(customerId)).thenReturn(Optional.of(wishlist));

        assertThatThrownBy(() -> service.execute(customerId, bookId))
                .isInstanceOf(WishlistItemAlreadyExistsException.class);

        verify(wishlistPersistencePort, never()).save(any());
    }

    private WishlistBookInfo buildBookInfo() {
        return new WishlistBookInfo("Clean Code", "/media/covers/clean-code.jpg",
                BigDecimal.valueOf(49.90), 10, true);
    }
}
