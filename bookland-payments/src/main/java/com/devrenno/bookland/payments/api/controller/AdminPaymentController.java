package com.devrenno.bookland.payments.api.controller;

import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final RefundPaymentUseCase refundPaymentUseCase;

    @PostMapping("/order/{orderId}/refund")
    public ResponseEntity<Void> refund(@PathVariable UUID orderId) {
        refundPaymentUseCase.refund(orderId);
        return ResponseEntity.noContent().build();
    }
}
