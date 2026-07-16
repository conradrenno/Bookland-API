package com.devrenno.bookland.orders.infrastructure.web;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrdersController ordersController;

    @GetMapping
    public ResponseEntity<PageResult<OrderSummaryViewModel>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        return ResponseEntity.ok(ordersController.getOrderHistory(customerId, PageQuery.of(page, size)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderViewModel> getById(
            @PathVariable UUID orderId,
            Principal principal
    ) {
        UUID requesterId = extractUserId(principal);
        return ResponseEntity.ok(ordersController.getOrderById(orderId, requesterId, false));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderViewModel> cancel(
            @PathVariable UUID orderId,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        return ResponseEntity.ok(ordersController.cancelOrder(orderId, customerId));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
