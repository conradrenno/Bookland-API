package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.mapper.CatalogApplicationMapper;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.entity.Category;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.IsbnAlreadyExistsException;
import com.devrenno.bookland.catalog.domain.service.CatalogDomainService;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBookServiceTest {

    @Mock private CatalogDomainService domainService;
    @Mock private BookPersistencePort bookPersistencePort;
    @Mock private CategoryPersistencePort categoryPersistencePort;
    @Mock private CatalogApplicationMapper mapper;
    @InjectMocks private CreateBookService createBookService;

    @Test
    void execute_shouldCreateBook_whenDataIsValid() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "9780132350884", List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1st", "A handbook of agile software craftsmanship.",
                new BigDecimal("89.90"), 10, categoryId
        );

        Category category = Category.of(CategoryId.of(categoryId), "Tecnologia", true);
        BookResponse expected = new BookResponse(UUID.randomUUID(), "Clean Code", "9780132350884",
                List.of("Robert C. Martin"), "Prentice Hall", 2008, "1st",
                "A handbook of agile software craftsmanship.", new BigDecimal("89.90"),
                10, true, categoryId, 0.0);

        when(categoryPersistencePort.findById(categoryId)).thenReturn(Optional.of(category));
        when(bookPersistencePort.existsByIsbn("9780132350884")).thenReturn(false);
        when(bookPersistencePort.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Book.class))).thenReturn(expected);

        BookResponse result = createBookService.execute(command);

        assertThat(result.title()).isEqualTo("Clean Code");
        assertThat(result.isbn()).isEqualTo("9780132350884");
        assertThat(result.available()).isTrue();
    }

    @Test
    void execute_shouldThrow_whenCategoryDoesNotExist() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "9780132350884", List.of("Robert C. Martin"),
                null, null, null, null, new BigDecimal("89.90"), 10, categoryId
        );

        when(categoryPersistencePort.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createBookService.execute(command))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void execute_shouldThrow_whenIsbnAlreadyExists() {
        UUID categoryId = UUID.randomUUID();
        CreateBookCommand command = new CreateBookCommand(
                "Clean Code", "9780132350884", List.of("Robert C. Martin"),
                null, null, null, null, new BigDecimal("89.90"), 10, categoryId
        );

        Category category = Category.of(CategoryId.of(categoryId), "Tecnologia", true);
        when(categoryPersistencePort.findById(categoryId)).thenReturn(Optional.of(category));
        when(bookPersistencePort.existsByIsbn("9780132350884")).thenReturn(true);
        doCallRealMethod().when(domainService).validateIsbnUniqueness(anyString(), anyBoolean());

        assertThatThrownBy(() -> createBookService.execute(command))
                .isInstanceOf(IsbnAlreadyExistsException.class);
    }
}
