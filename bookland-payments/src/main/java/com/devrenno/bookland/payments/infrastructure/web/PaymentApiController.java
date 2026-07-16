package com.devrenno.bookland.payments.infrastructure.web;

import com.devrenno.bookland.payments.adapters.controller.PaymentController;
import com.devrenno.bookland.payments.adapters.viewmodel.PaymentViewModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentController paymentController;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentViewModel> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentController.getByOrderId(orderId));
    }
}
