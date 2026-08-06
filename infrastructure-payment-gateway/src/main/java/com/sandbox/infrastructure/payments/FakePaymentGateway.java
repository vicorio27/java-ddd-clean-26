package com.sandbox.infrastructure.payments;

import com.sandbox.payments.domain.model.Payment;
import com.sandbox.payments.domain.port.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);
    private static final BigDecimal AUTO_APPROVAL_LIMIT = new BigDecimal("5000");

    @Override
    public PaymentGatewayResult charge(Payment payment) {
        log.info("Charging payment {} amount {}", payment.id().value(), payment.amount().amount());
        if (payment.amount().amount().compareTo(AUTO_APPROVAL_LIMIT) <= 0) {
            return new PaymentGatewayResult(true, UUID.randomUUID().toString(), null);
        }
        return new PaymentGatewayResult(false, null, "Amount exceeds auto-approval limit");
    }
}
