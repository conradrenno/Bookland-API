package com.devrenno.bookland.orders.api.controller;

import com.devrenno.bookland.orders.api.dto.request.AddCartItemRequest;
import com.devrenno.bookland.orders.api.dto.request.CheckoutRequest;
import com.devrenno.bookland.orders.api.dto.request.UpdateCartItemRequest;
import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.CheckoutUseCase;
import com.devrenno.bookland.orders.application.port.in.GetCartUseCase;
import com.devrenno.bookland.orders.application.port.in.RemoveCartItemUseCase;
import com.devrenno.bookland.orders.application.port.in.UpdateCartItemUseCase;
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
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddCartItemUseCase addCartItemUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final CheckoutUseCase checkoutUseCase;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Principal principal) {
        return ResponseEntity.ok(getCartUseCase.execute(extractUserId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        CartResponse response = addCartItemUseCase.execute(
                new AddCartItemCommand(customerId, request.bookId(), request.quantity())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        CartResponse response = updateCartItemUseCase.execute(
                new UpdateCartItemCommand(customerId, bookId, request.quantity())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable UUID bookId,
            Principal principal
    ) {
        return ResponseEntity.ok(removeCartItemUseCase.execute(extractUserId(principal), bookId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(checkoutUseCase.execute(extractUserId(principal), request.paymentMethod()));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
