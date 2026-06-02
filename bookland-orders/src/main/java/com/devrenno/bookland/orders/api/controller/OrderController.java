package com.devrenno.bookland.orders.api.controller;

import com.devrenno.bookland.orders.api.dto.response.PagedOrderSummaryResponse;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.port.in.CancelOrderUseCase;
import com.devrenno.bookland.orders.application.port.in.GetOrderByIdUseCase;
import com.devrenno.bookland.orders.application.port.in.GetOrderHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final GetOrderHistoryUseCase getOrderHistoryUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    @GetMapping
    public ResponseEntity<PagedOrderSummaryResponse> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        var result = getOrderHistoryUseCase.execute(customerId, PageRequest.of(page, size));
        return ResponseEntity.ok(new PagedOrderSummaryResponse(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(
            @PathVariable UUID orderId,
            Principal principal
    ) {
        UUID requesterId = extractUserId(principal);
        return ResponseEntity.ok(getOrderByIdUseCase.execute(orderId, requesterId, false));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable UUID orderId,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        return ResponseEntity.ok(cancelOrderUseCase.execute(orderId, customerId));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
