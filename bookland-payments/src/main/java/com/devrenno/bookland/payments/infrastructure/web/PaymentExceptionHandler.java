package com.devrenno.bookland.payments.infrastructure.web;

import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
import com.devrenno.bookland.websupport.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(PaymentNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "PAYMENT_NOT_FOUND");
    }

    @ExceptionHandler(RefundNotAllowedException.class)
    public ProblemDetail handleRefundNotAllowed(RefundNotAllowedException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "REFUND_NOT_ALLOWED");
    }
}
