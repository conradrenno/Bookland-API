package com.devrenno.bookland.orders.api.controller;

import com.devrenno.bookland.orders.api.dto.request.UpdateOrderStatusRequest;
import com.devrenno.bookland.orders.api.dto.response.PagedOrderSummaryResponse;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.in.GetOrderByIdUseCase;
import com.devrenno.bookland.orders.application.port.in.GetOrderHistoryUseCase;
import com.devrenno.bookland.orders.application.port.in.UpdateOrderStatusUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final GetOrderHistoryUseCase getOrderHistoryUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(getOrderByIdUseCase.execute(orderId, null, true));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PagedOrderSummaryResponse> getCustomerHistory(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = getOrderHistoryUseCase.execute(customerId, PageRequest.of(page, size));
        return ResponseEntity.ok(new PagedOrderSummaryResponse(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        ));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Principal principal
    ) {
        UUID adminId = extractUserId(principal);
        return ResponseEntity.ok(updateOrderStatusUseCase.execute(
                new UpdateOrderStatusCommand(orderId, request.newStatus(), adminId)
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
