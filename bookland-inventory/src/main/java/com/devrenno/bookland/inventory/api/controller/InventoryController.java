package com.devrenno.bookland.inventory.api.controller;

import com.devrenno.bookland.inventory.api.dto.response.PagedLowStockBookApiResponse;
import com.devrenno.bookland.inventory.api.mapper.InventoryApiMapper;
import com.devrenno.bookland.inventory.application.port.in.GetLowStockBooksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final GetLowStockBooksUseCase getLowStockBooksUseCase;
    private final InventoryApiMapper mapper;

    @GetMapping("/low-stock")
    public ResponseEntity<PagedLowStockBookApiResponse> lowStock(
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = getLowStockBooksUseCase.execute(
                threshold, PageRequest.of(page, size, Sort.by("stockQuantity").ascending())
        );
        return ResponseEntity.ok(new PagedLowStockBookApiResponse(
                result.getContent().stream().map(mapper::toApiResponse).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        ));
    }
}
