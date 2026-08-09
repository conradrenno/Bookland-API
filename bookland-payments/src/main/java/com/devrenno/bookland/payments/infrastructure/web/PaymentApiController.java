package com.devrenno.bookland.payments.infrastructure.web;

import com.devrenno.bookland.payments.adapters.controller.PaymentController;
import com.devrenno.bookland.payments.adapters.viewmodel.PaymentViewModel;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * A customer reading the payment of their own order. Refunds and any back-office view live on
 * {@code /api/v1/admin/payments/**} with their own {@code hasRole("ADMIN")} rule.
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
