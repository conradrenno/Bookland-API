package com.devrenno.bookland.catalog.infrastructure.web;

import com.devrenno.bookland.catalog.adapters.controller.CatalogController;
import com.devrenno.bookland.catalog.adapters.viewmodel.BookViewModel;
import com.devrenno.bookland.catalog.application.common.BookSort;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.application.dto.UploadBookCoverCommand;
import com.devrenno.bookland.catalog.infrastructure.web.dto.CreateBookRequest;
import com.devrenno.bookland.catalog.infrastructure.web.dto.UpdateBookRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookApiController {

    private final CatalogController catalogController;
    private final CatalogRequestMapper requestMapper;

    @PostMapping
    public ResponseEntity<BookViewModel> create(@Valid @RequestBody CreateBookRequest request) {
        BookViewModel book = catalogController.createBook(requestMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(book.id())
                .toUri();
        return ResponseEntity.created(location).body(book);
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookViewModel> getById(@PathVariable UUID bookId) {
        return ResponseEntity.ok(catalogController.getBook(bookId));
    }

    @GetMapping
    public ResponseEntity<PageResult<BookViewModel>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        BookSearchQuery query = new BookSearchQuery(q, category, minPrice, maxPrice, page, size, resolveSort(sort));
        return ResponseEntity.ok(catalogController.search(query));
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<BookViewModel> update(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        return ResponseEntity.ok(catalogController.updateBook(bookId, requestMapper.toCommand(request)));
    }

    @PostMapping(value = "/{bookId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookViewModel> uploadCover(
            @PathVariable UUID bookId,
            @RequestParam("file") MultipartFile file
    ) {
        UploadBookCoverCommand command = new UploadBookCoverCommand(
                readBytes(file), file.getOriginalFilename(), file.getContentType());
        return ResponseEntity.ok(catalogController.uploadBookCover(bookId, command));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> remove(@PathVariable UUID bookId) {
        catalogController.removeBook(bookId);
        return ResponseEntity.noContent().build();
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    private BookSort resolveSort(String sort) {
        return switch (sort) {
            case "price" -> BookSort.PRICE_ASC;
            case "price_desc" -> BookSort.PRICE_DESC;
            case "avgRating" -> BookSort.RATING_DESC;
            default -> BookSort.TITLE;
        };
    }
}
