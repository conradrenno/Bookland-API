package com.devrenno.bookland.payments.infrastructure.web;

import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin refund endpoint. Refund is a cross-module boundary use case (also consumed by orders), so
 * this thin HTTP wrapper delegates straight to the use case — there is no view model to present.
 */
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentApiController {

    private final RefundPaymentUseCase refundPaymentUseCase;

    @PostMapping("/order/{orderId}/refund")
    public ResponseEntity<Void> refund(@PathVariable UUID orderId) {
        refundPaymentUseCase.refund(orderId);
        return ResponseEntity.noContent().build();
    }
}
