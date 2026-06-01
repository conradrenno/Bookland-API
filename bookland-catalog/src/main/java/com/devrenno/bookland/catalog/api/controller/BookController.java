package com.devrenno.bookland.catalog.api.controller;

import com.devrenno.bookland.catalog.api.dto.request.CreateBookRequest;
import com.devrenno.bookland.catalog.api.dto.request.UpdateBookRequest;
import com.devrenno.bookland.catalog.api.dto.response.BookApiResponse;
import com.devrenno.bookland.catalog.api.dto.response.PagedBookApiResponse;
import com.devrenno.bookland.catalog.api.mapper.CatalogApiMapper;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final CreateBookUseCase createBookUseCase;
    private final GetBookByIdUseCase getBookByIdUseCase;
    private final SearchBooksUseCase searchBooksUseCase;
    private final UpdateBookUseCase updateBookUseCase;
    private final RemoveBookUseCase removeBookUseCase;
    private final CatalogApiMapper mapper;

    @PostMapping
    public ResponseEntity<BookApiResponse> create(@Valid @RequestBody CreateBookRequest request) {
        BookResponse response = createBookUseCase.execute(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toApiResponse(response));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookApiResponse> getById(@PathVariable UUID bookId) {
        return ResponseEntity.ok(mapper.toApiResponse(getBookByIdUseCase.execute(bookId)));
    }

    @GetMapping
    public ResponseEntity<PagedBookApiResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Sort sorting = resolveSort(sort);
        BookSearchQuery query = new BookSearchQuery(q, category, minPrice, maxPrice,
                PageRequest.of(page, size, sorting));

        Page<BookResponse> result = searchBooksUseCase.execute(query);
        PagedBookApiResponse body = new PagedBookApiResponse(
                result.getContent().stream().map(mapper::toApiResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<BookApiResponse> update(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        return ResponseEntity.ok(mapper.toApiResponse(updateBookUseCase.execute(bookId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> remove(@PathVariable UUID bookId) {
        removeBookUseCase.execute(bookId);
        return ResponseEntity.noContent().build();
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "price" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "avgRating" -> Sort.by("avgRating").descending();
            default -> Sort.by("title").ascending();
        };
    }
}
