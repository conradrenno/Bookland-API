package com.devrenno.bookland.orders.infrastructure.web;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrdersController ordersController;

    @GetMapping
    public ResponseEntity<PageResult<OrderSummaryViewModel>> getHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.getOrderHistory(caller.id(), PageQuery.of(page, size)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderViewModel> getById(
            @PathVariable UUID orderId,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.getOrderById(orderId, caller.id(), false));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderViewModel> cancel(
            @PathVariable UUID orderId,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.cancelOrder(orderId, caller.id()));
    }
}
