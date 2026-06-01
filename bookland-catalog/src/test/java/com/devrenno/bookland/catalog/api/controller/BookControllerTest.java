package com.devrenno.bookland.catalog.api.controller;

import com.devrenno.bookland.catalog.api.dto.response.BookApiResponse;
import com.devrenno.bookland.catalog.api.mapper.CatalogApiMapper;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.port.in.*;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock CreateBookUseCase createBookUseCase;
    @Mock GetBookByIdUseCase getBookByIdUseCase;
    @Mock SearchBooksUseCase searchBooksUseCase;
    @Mock UpdateBookUseCase updateBookUseCase;
    @Mock RemoveBookUseCase removeBookUseCase;
    @Mock CatalogApiMapper catalogApiMapper;
    @InjectMocks BookController bookController;

    private static final UUID BOOK_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private BookResponse bookResponse() {
        return new BookResponse(BOOK_ID, "Clean Code", "9780132350884",
                List.of("Robert C. Martin"), "Prentice Hall", 2008, "1st",
                "Synopsis", new BigDecimal("89.90"), 10, true, CATEGORY_ID, 0.0);
    }

    private BookApiResponse bookApiResponse() {
        return new BookApiResponse(BOOK_ID, "Clean Code", "9780132350884",
                List.of("Robert C. Martin"), "Prentice Hall", 2008, "1st",
                "Synopsis", new BigDecimal("89.90"), 10, true, CATEGORY_ID, 0.0);
    }

    @Test
    void getById_shouldReturn200_whenBookExists() {
        when(getBookByIdUseCase.execute(BOOK_ID)).thenReturn(bookResponse());
        when(catalogApiMapper.toApiResponse(any(BookResponse.class))).thenReturn(bookApiResponse());

        ResponseEntity<BookApiResponse> result = bookController.getById(BOOK_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().title()).isEqualTo("Clean Code");
        assertThat(result.getBody().isbn()).isEqualTo("9780132350884");
    }

    @Test
    void getById_shouldPropagateException_whenBookDoesNotExist() {
        when(getBookByIdUseCase.execute(BOOK_ID)).thenThrow(new BookNotFoundException(BOOK_ID));

        assertThatThrownBy(() -> bookController.getById(BOOK_ID))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void remove_shouldReturn204_whenBookIsDeleted() {
        ResponseEntity<Void> result = bookController.remove(BOOK_ID);

        verify(removeBookUseCase).execute(BOOK_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
