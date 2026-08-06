package com.sandbox.application.command;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderCommand(String customerId, List<Line> lines) {

    public record Line(String productId, int quantity, BigDecimal unitPrice, String currency) {
    }
}
