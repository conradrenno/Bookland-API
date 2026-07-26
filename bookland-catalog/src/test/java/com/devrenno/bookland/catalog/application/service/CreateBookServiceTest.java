package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.entity.Category;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.IsbnAlreadyExistsException;
import com.devrenno.bookland.catalog.domain.service.CatalogDomainService;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBookServiceTest {

    @Mock private BookPersistencePort bookPersistencePort;
    @Mock private CategoryPersistencePort categoryPersistencePort;

    private CreateBookService createBookService;

    @BeforeEach
    void setUp() {
        createBookService = CreateBookService.create(
                new CatalogDomainService(), bookPersistencePort, categoryPersistencePort);
    }

    @Test
    void execute_shouldCreateBook_whenDataIsValid() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "9780132350884", List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1st", "A handbook of agile software craftsmanship.",
                new BigDecimal("89.90"), 10, categoryId, "https://covers.example.com/clean-code.jpg"
        );

        Category category = Category.of(CategoryId.of(categoryId), "Tecnologia", true);

        when(categoryPersistencePort.findById(categoryId)).thenReturn(Optional.of(category));
        when(bookPersistencePort.existsByIsbn("9780132350884")).thenReturn(false);
        when(bookPersistencePort.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = createBookService.execute(command);

        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getIsbn().value()).isEqualTo("9780132350884");
        assertThat(result.getStockQuantity()).isEqualTo(10);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getCategoryId().value()).isEqualTo(categoryId);
        assertThat(result.getCoverImageUrl()).isEqualTo("https://covers.example.com/clean-code.jpg");
    }

    @Test
    void execute_shouldThrow_whenCategoryDoesNotExist() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "9780132350884", List.of("Robert C. Martin"),
                null, null, null, null, new BigDecimal("89.90"), 10, categoryId, null
        );

        when(categoryPersistencePort.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createBookService.execute(command))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void execute_shouldCheckUniquenessAndStoreTheCanonicalIsbn_whenInputIsHyphenated() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "978-0-13-235088-4", List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1st", "A handbook of agile software craftsmanship.",
                new BigDecimal("89.90"), 10, categoryId, null
        );

        when(categoryPersistencePort.findById(categoryId))
                .thenReturn(Optional.of(Category.of(CategoryId.of(categoryId), "Tecnologia", true)));
        when(bookPersistencePort.existsByIsbn("9780132350884")).thenReturn(false);
        when(bookPersistencePort.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(createBookService.execute(command).getIsbn().value()).isEqualTo("9780132350884");
    }

    @Test
    void execute_shouldThrow_whenIsbnAlreadyExists() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "9780132350884", List.of("Robert C. Martin"),
                null, null, null, null, new BigDecimal("89.90"), 10, categoryId, null
        );

        Category category = Category.of(CategoryId.of(categoryId), "Tecnologia", true);
        when(categoryPersistencePort.findById(categoryId)).thenReturn(Optional.of(category));
        when(bookPersistencePort.existsByIsbn("9780132350884")).thenReturn(true);

        assertThatThrownBy(() -> createBookService.execute(command))
                .isInstanceOf(IsbnAlreadyExistsException.class);
    }
}
