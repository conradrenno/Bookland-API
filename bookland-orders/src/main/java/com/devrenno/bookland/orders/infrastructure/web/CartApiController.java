package com.devrenno.bookland.orders.infrastructure.web;

import com.devrenno.bookland.orders.adapters.controller.OrdersController;
import com.devrenno.bookland.orders.adapters.viewmodel.CartViewModel;
import com.devrenno.bookland.orders.adapters.viewmodel.OrderViewModel;
import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;
import com.devrenno.bookland.orders.infrastructure.web.dto.AddCartItemRequest;
import com.devrenno.bookland.orders.infrastructure.web.dto.CheckoutRequest;
import com.devrenno.bookland.orders.infrastructure.web.dto.UpdateCartItemRequest;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final OrdersController ordersController;

    @GetMapping
    public ResponseEntity<CartViewModel> getCart(AuthenticatedUser caller) {
        return ResponseEntity.ok(ordersController.getCart(caller.id()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartViewModel> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.addCartItem(
                new AddCartItemCommand(caller.id(), request.bookId(), request.quantity())
        ));
    }

    @PatchMapping("/items/{bookId}")
    public ResponseEntity<CartViewModel> updateItem(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateCartItemRequest request,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.updateCartItem(
                new UpdateCartItemCommand(caller.id(), bookId, request.quantity())
        ));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartViewModel> removeItem(
            @PathVariable UUID bookId,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.removeCartItem(caller.id(), bookId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderViewModel> checkout(
            @Valid @RequestBody CheckoutRequest request,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(ordersController.checkout(caller.id(), request.paymentMethod()));
    }
}
