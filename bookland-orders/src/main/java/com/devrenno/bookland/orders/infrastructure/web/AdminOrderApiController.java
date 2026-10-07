package com.devrenno.bookland.orders.infrastructure.web;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.adapters.viewmodel.AdminOrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderSummaryViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.infrastructure.web.dto.UpdateOrderStatusRequest;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderApiController {

    private final OrdersController ordersController;

    @GetMapping
    public ResponseEntity<PageResult<AdminOrderSummaryViewModel>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return ResponseEntity.ok(ordersController.listAllOrders(status, PageQuery.of(page, size)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderViewModel> getById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ordersController.getOrderById(orderId, null, true));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PageResult<OrderSummaryViewModel>> getCustomerHistory(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return ResponseEntity.ok(ordersController.getOrderHistory(customerId, PageQuery.of(page, size)));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderViewModel> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            AuthenticatedUser admin
    ) {
        return ResponseEntity.ok(ordersController.updateOrderStatus(
                new UpdateOrderStatusCommand(orderId, request.newStatus(), admin.id())
        ));
    }
}
