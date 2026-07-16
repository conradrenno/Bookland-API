package com.devrenno.bookland.inventory.infrastructure.web;

import com.devrenno.bookland.inventory.adapters.controller.InventoryController;
import com.devrenno.bookland.inventory.adapters.viewmodel.InventoryEntryViewModel;
import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.infrastructure.web.dto.AdjustInventoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}/inventory")
@RequiredArgsConstructor
public class BookInventoryApiController {

    private final InventoryController inventoryController;

    @PatchMapping
    public ResponseEntity<InventoryEntryViewModel> adjust(
            @PathVariable UUID bookId,
            @Valid @RequestBody AdjustInventoryRequest request,
            Principal principal
    ) {
        AdjustInventoryCommand command = new AdjustInventoryCommand(
                bookId, request.delta(), request.reason(), extractUserId(principal)
        );
        return ResponseEntity.ok(inventoryController.adjust(command));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResult<InventoryEntryViewModel>> history(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(inventoryController.history(bookId, PageQuery.of(page, size)));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
