package com.sandbox.orders.domain.model;

import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.id.CustomerId;
import com.sandbox.shared.kernel.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final OrderLine LINE = new OrderLine("SKU-1", 2, Money.of(new BigDecimal("10.00"), "USD"));

    @Test
    void createsOrderWithTotalAndEvent() {
        var order = Order.create(CustomerId.newId(), List.of(LINE));

        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.total().amount()).isEqualByComparingTo("20.00");
        assertThat(order.pullDomainEvents()).hasSize(1);
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> Order.create(CustomerId.newId(), List.of()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsIllegalStateTransition() {
        var order = Order.create(CustomerId.newId(), List.of(LINE));

        assertThatThrownBy(order::markAsPaid)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Illegal order state transition");
    }
}
