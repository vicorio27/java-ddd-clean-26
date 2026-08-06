package com.sandbox.orders.domain.service;

import com.sandbox.orders.domain.model.Order;
import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.money.Money;

import java.math.BigDecimal;

public class OrderDomainService {

    private final Money approvalThreshold;

    public OrderDomainService(Money approvalThreshold) {
        this.approvalThreshold = approvalThreshold;
    }

    public boolean requiresManualApproval(Order order) {
        return order.total().amount().compareTo(approvalThreshold.amount()) > 0;
    }

    public void assertCanBeCreated(Order order) {
        if (order.total().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Order total must be greater than zero");
        }
    }
}
