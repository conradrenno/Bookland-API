package com.devrenno.bookland.catalog.infrastructure.web;

import com.devrenno.bookland.catalog.adapters.controller.CatalogController;
import com.devrenno.bookland.catalog.adapters.viewmodel.BookViewModel;
import com.devrenno.bookland.catalog.adapters.viewmodel.CategoryViewModel;
import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryApiController {

    private final CatalogController catalogController;

    @GetMapping
    public ResponseEntity<List<CategoryViewModel>> listCategories() {
        return ResponseEntity.ok(catalogController.listCategories());
    }

    @GetMapping("/{categoryId}/books")
    public ResponseEntity<PageResult<BookViewModel>> listBooksByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(catalogController.listBooksByCategory(categoryId, PageQuery.of(page, size)));
    }
}
