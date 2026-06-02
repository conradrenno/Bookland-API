package com.devrenno.bookland.payments.application.port.in;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;

public interface ProcessPaymentUseCase {
    PaymentResult processPayment(ProcessPaymentCommand command);
}
