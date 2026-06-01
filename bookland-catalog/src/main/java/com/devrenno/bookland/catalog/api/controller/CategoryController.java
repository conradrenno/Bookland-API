package com.devrenno.bookland.catalog.api.controller;

import com.devrenno.bookland.catalog.api.dto.response.BookApiResponse;
import com.devrenno.bookland.catalog.api.dto.response.CategoryApiResponse;
import com.devrenno.bookland.catalog.api.dto.response.PagedBookApiResponse;
import com.devrenno.bookland.catalog.api.mapper.CatalogApiMapper;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.port.in.ListBooksByCategoryUseCase;
import com.devrenno.bookland.catalog.application.port.in.ListCategoriesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final ListBooksByCategoryUseCase listBooksByCategoryUseCase;
    private final CatalogApiMapper mapper;

    @GetMapping
    public ResponseEntity<List<CategoryApiResponse>> listCategories() {
        List<CategoryApiResponse> body = listCategoriesUseCase.execute().stream()
                .map(mapper::toApiResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{categoryId}/books")
    public ResponseEntity<PagedBookApiResponse> listBooksByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BookResponse> result = listBooksByCategoryUseCase.execute(
                categoryId, PageRequest.of(page, size, Sort.by("title").ascending()));

        PagedBookApiResponse body = new PagedBookApiResponse(
                result.getContent().stream().map(mapper::toApiResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ResponseEntity.ok(body);
    }
}
