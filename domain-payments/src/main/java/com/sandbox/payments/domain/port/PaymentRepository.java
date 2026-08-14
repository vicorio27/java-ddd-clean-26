package com.sandbox.payments.domain.port;

import com.sandbox.payments.domain.model.Payment;
import com.sandbox.payments.domain.model.PaymentId;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByOrderReference(String orderReference);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
