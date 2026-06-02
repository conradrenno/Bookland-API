package com.devrenno.bookland.payments.application.port.out;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;

public interface PaymentGatewayPort {
    PaymentResult charge(ProcessPaymentCommand command);
    void refund(String transactionId);
}
