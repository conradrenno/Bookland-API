package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBookByIdServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;

    private GetBookByIdService service;

    @BeforeEach
    void setUp() {
        service = GetBookByIdService.create(bookPersistencePort);
    }

    private Book buildBook() {
        return Book.create(
                "Clean Code", ISBN.of("9780132350884"), List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1st", "Synopsis",
                Price.of(new BigDecimal("89.90")), 10, CategoryId.of(UUID.randomUUID()),
                "/media/covers/clean-code.jpg"
        );
    }

    @Test
    void execute_shouldReturnBook_whenActive() {
        UUID bookId = UUID.randomUUID();
        Book book = buildBook();
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(book));

        assertThat(service.execute(bookId)).isSameAs(book);
    }

    @Test
    void execute_shouldThrowNotFound_whenBookIsDeactivated() {
        UUID bookId = UUID.randomUUID();
        Book book = buildBook();
        book.deactivate();
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.execute(bookId))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void execute_shouldThrowNotFound_whenBookDoesNotExist() {
        UUID bookId = UUID.randomUUID();
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(bookId))
                .isInstanceOf(BookNotFoundException.class);
    }
}
