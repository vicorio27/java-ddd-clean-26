package com.sandbox.payments.domain.port;

import com.sandbox.payments.domain.model.Payment;

public interface PaymentGateway {

    PaymentGatewayResult charge(Payment payment);

    record PaymentGatewayResult(boolean approved, String authorizationCode, String rejectionReason) {
    }
}
