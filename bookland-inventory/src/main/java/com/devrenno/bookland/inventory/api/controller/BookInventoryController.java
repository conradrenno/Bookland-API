package com.devrenno.bookland.inventory.api.controller;

import com.devrenno.bookland.inventory.api.dto.request.AdjustInventoryRequest;
import com.devrenno.bookland.inventory.api.dto.response.InventoryEntryApiResponse;
import com.devrenno.bookland.inventory.api.dto.response.PagedInventoryEntryApiResponse;
import com.devrenno.bookland.inventory.api.mapper.InventoryApiMapper;
import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.port.in.AdjustInventoryUseCase;
import com.devrenno.bookland.inventory.application.port.in.GetInventoryHistoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}/inventory")
@RequiredArgsConstructor
public class BookInventoryController {

    private final AdjustInventoryUseCase adjustInventoryUseCase;
    private final GetInventoryHistoryUseCase getInventoryHistoryUseCase;
    private final InventoryApiMapper mapper;

    @PatchMapping
    public ResponseEntity<InventoryEntryApiResponse> adjust(
            @PathVariable UUID bookId,
            @Valid @RequestBody AdjustInventoryRequest request,
            Principal principal
    ) {
        UUID adjustedBy = extractUserId(principal);
        AdjustInventoryCommand command = new AdjustInventoryCommand(
                bookId, request.delta(), request.reason(), adjustedBy
        );
        return ResponseEntity.ok(mapper.toApiResponse(adjustInventoryUseCase.execute(command)));
    }

    @GetMapping("/history")
    public ResponseEntity<PagedInventoryEntryApiResponse> history(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = getInventoryHistoryUseCase.execute(bookId, PageRequest.of(page, size));
        return ResponseEntity.ok(new PagedInventoryEntryApiResponse(
                result.getContent().stream().map(mapper::toApiResponse).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        ));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
