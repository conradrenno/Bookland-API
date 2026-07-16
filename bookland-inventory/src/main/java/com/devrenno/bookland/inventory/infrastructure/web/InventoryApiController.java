package com.devrenno.bookland.inventory.infrastructure.web;

import com.devrenno.bookland.inventory.adapters.controller.InventoryController;
import com.devrenno.bookland.inventory.adapters.viewmodel.LowStockBookViewModel;
import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryApiController {

    private final InventoryController inventoryController;

    @GetMapping("/low-stock")
    public ResponseEntity<PageResult<LowStockBookViewModel>> lowStock(
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(inventoryController.lowStock(threshold, PageQuery.of(page, size)));
    }
}
