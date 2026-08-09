package com.devrenno.bookland.payments.infrastructure.web;

import com.devrenno.bookland.payments.adapters.controller.PaymentController;
import com.devrenno.bookland.payments.adapters.viewmodel.PaymentViewModel;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * A customer reading the payment of their own order — the module's only route. A refund is not
 * exposed here: it is one half of a cancellation, and reaching it on its own left the order
 * CONFIRMED with the stock never returned. The whole operation is
 * {@code PATCH /api/v1/admin/orders/{id}/status} → CANCELLED, which compensates through
 * {@code OrderCancellation}.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentController paymentController;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentViewModel> getByOrderId(@PathVariable UUID orderId,
                                                         AuthenticatedUser caller) {
        return ResponseEntity.ok(paymentController.getByOrderId(orderId, caller.id()));
    }
}
