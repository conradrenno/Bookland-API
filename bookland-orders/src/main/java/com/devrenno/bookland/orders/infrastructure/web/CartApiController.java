package com.devrenno.bookland.orders.infrastructure.web;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.adapters.viewmodel.CartViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;
import com.devrenno.bookland.orders.infrastructure.web.dto.AddCartItemRequest;
import com.devrenno.bookland.orders.infrastructure.web.dto.CheckoutRequest;
import com.devrenno.bookland.orders.infrastructure.web.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final OrdersController ordersController;

    @GetMapping
    public ResponseEntity<CartViewModel> getCart(Principal principal) {
        return ResponseEntity.ok(ordersController.getCart(extractUserId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartViewModel> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        return ResponseEntity.ok(ordersController.addCartItem(
                new AddCartItemCommand(customerId, request.bookId(), request.quantity())
        ));
    }

    @PatchMapping("/items/{bookId}")
    public ResponseEntity<CartViewModel> updateItem(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        return ResponseEntity.ok(ordersController.updateCartItem(
                new UpdateCartItemCommand(customerId, bookId, request.quantity())
        ));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartViewModel> removeItem(
            @PathVariable UUID bookId,
            Principal principal
    ) {
        return ResponseEntity.ok(ordersController.removeCartItem(extractUserId(principal), bookId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderViewModel> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(ordersController.checkout(extractUserId(principal), request.paymentMethod()));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
