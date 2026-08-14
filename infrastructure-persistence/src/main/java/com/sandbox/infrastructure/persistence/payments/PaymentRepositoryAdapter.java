package com.sandbox.infrastructure.persistence.payments;

import com.sandbox.payments.domain.model.Payment;
import com.sandbox.payments.domain.model.PaymentId;
import com.sandbox.payments.domain.model.PaymentStatus;
import com.sandbox.payments.domain.port.PaymentRepository;
import com.sandbox.shared.kernel.money.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        return toDomain(jpaRepository.save(toEntity(payment)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderReference(String orderReference) {
        return jpaRepository.findByOrderReference(orderReference).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private PaymentJpaEntity toEntity(Payment payment) {
        var entity = new PaymentJpaEntity();
        entity.setId(payment.id().value());
        entity.setOrderReference(payment.orderReference());
        entity.setAmount(payment.amount().amount());
        entity.setCurrency(payment.amount().currency().getCurrencyCode());
        entity.setStatus(payment.status().name());
        entity.setIdempotencyKey(payment.idempotencyKey());
        return entity;
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(new PaymentId(entity.getId()), entity.getOrderReference(),
                Money.of(entity.getAmount(), entity.getCurrency()), entity.getIdempotencyKey(),
                PaymentStatus.valueOf(entity.getStatus()));
    }
}
