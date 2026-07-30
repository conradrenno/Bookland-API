package com.devrenno.bookland.catalog.adapters;

import com.devrenno.bookland.catalog.adapters.controller.CatalogController;
import com.devrenno.bookland.catalog.adapters.viewmodel.BookViewModel;
import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.valueobject.BookId;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock private BookPersistencePort bookPersistencePort;
    @Mock private CategoryPersistencePort categoryPersistencePort;
    @Mock private ActiveOrderCheckPort activeOrderCheckPort;
    @Mock private com.devrenno.bookland.catalog.application.port.out.ImageStoragePort imageStoragePort;

    private CatalogController controller;

    private static final UUID BOOK_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = CatalogController.create(
                bookPersistencePort, categoryPersistencePort, activeOrderCheckPort, imageStoragePort);
    }

    private Book sampleBook() {
        Instant now = Instant.now();
        return Book.reconstitute(
                BookId.of(BOOK_ID), "Clean Code", ISBN.of("9780132350884"),
                List.of("Robert C. Martin"), "Prentice Hall", 2008, "1st", "Synopsis",
                Price.of(new BigDecimal("89.90")), 10, CategoryId.of(CATEGORY_ID),
                "https://covers.example.com/clean-code.jpg", 0.0, true, now, now);
    }

    @Test
    void getBook_shouldReturnViewModel_whenBookExists() {
        when(bookPersistencePort.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook()));

        BookViewModel result = controller.getBook(BOOK_ID);

        assertThat(result.id()).isEqualTo(BOOK_ID);
        assertThat(result.title()).isEqualTo("Clean Code");
        assertThat(result.isbn()).isEqualTo("9780132350884");
        assertThat(result.available()).isTrue();
        assertThat(result.categoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void getBook_shouldThrow_whenBookDoesNotExist() {
        when(bookPersistencePort.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getBook(BOOK_ID))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void removeBook_shouldSoftDelete_whenNoActiveOrders() {
        when(bookPersistencePort.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook()));
        when(activeOrderCheckPort.hasActiveOrdersForBook(BOOK_ID)).thenReturn(false);
        when(bookPersistencePort.save(org.mockito.ArgumentMatchers.any(Book.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        controller.removeBook(BOOK_ID);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookPersistencePort).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }
}
