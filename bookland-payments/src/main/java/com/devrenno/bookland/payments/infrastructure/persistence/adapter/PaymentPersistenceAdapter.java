package com.devrenno.bookland.payments.infrastructure.persistence.adapter;

import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import com.devrenno.bookland.payments.infrastructure.persistence.entity.PaymentJpaEntity;
import com.devrenno.bookland.payments.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private static final List<String> AWAITING_GATEWAY =
            List.of(PaymentStatus.PENDING.name(), PaymentStatus.REFUND_PENDING.name());

    private final PaymentJpaRepository repository;

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = toEntity(payment);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    public List<Payment> findDue(Instant now, int limit) {
        return repository.findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        AWAITING_GATEWAY, now, Limit.of(limit))
                .stream().map(this::toDomain).toList();
    }

    /** last_error is varchar(500); an exception message can be longer, and must not fail the save. */
    private static String fitLastError(String error) {
        return error == null || error.length() <= 500 ? error : error.substring(0, 500);
    }

    private PaymentJpaEntity toEntity(Payment payment) {
        return PaymentJpaEntity.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .status(payment.getStatus().name())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .declineReason(payment.getDeclineReason())
                .attempts(payment.getAttempts())
                .nextAttemptAt(payment.getNextAttemptAt())
                .lastError(fitLastError(payment.getLastError()))
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getAmount(),
                PaymentMethod.valueOf(entity.getMethod()),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getGatewayTransactionId(),
                entity.getDeclineReason(),
                entity.getAttempts(),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
