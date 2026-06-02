package com.devrenno.bookland.payments.api.controller;

import com.devrenno.bookland.payments.application.dto.PaymentResponse;
import com.devrenno.bookland.payments.application.port.in.GetPaymentByOrderIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final GetPaymentByOrderIdUseCase getPaymentByOrderIdUseCase;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(getPaymentByOrderIdUseCase.getByOrderId(orderId));
    }
}
