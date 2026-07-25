package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookHasActiveOrdersException;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveBookServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;
    @Mock private ActiveOrderCheckPort activeOrderCheckPort;

    private RemoveBookService removeBookService;

    @BeforeEach
    void setUp() {
        removeBookService = RemoveBookService.create(bookPersistencePort, activeOrderCheckPort);
    }

    private Book buildBook() {
        return Book.create(
                "Clean Code", ISBN.of("9780132350884"), List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1st", "Synopsis",
                Price.of(new BigDecimal("89.90")), 10, CategoryId.of(UUID.randomUUID()), null
        );
    }

    @Test
    void execute_shouldSoftDeleteBook_whenNoActiveOrders() {
        UUID bookId = UUID.randomUUID();
        Book book = buildBook();

        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(book));
        when(activeOrderCheckPort.hasActiveOrdersForBook(bookId)).thenReturn(false);
        when(bookPersistencePort.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        removeBookService.execute(bookId);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookPersistencePort).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void execute_shouldThrow_whenBookDoesNotExist() {
        UUID bookId = UUID.randomUUID();
        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> removeBookService.execute(bookId))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void execute_shouldThrow_whenBookHasActiveOrders() {
        UUID bookId = UUID.randomUUID();
        Book book = buildBook();

        when(bookPersistencePort.findById(bookId)).thenReturn(Optional.of(book));
        when(activeOrderCheckPort.hasActiveOrdersForBook(bookId)).thenReturn(true);

        assertThatThrownBy(() -> removeBookService.execute(bookId))
                .isInstanceOf(BookHasActiveOrdersException.class);

        verify(bookPersistencePort, never()).save(any());
    }
}
