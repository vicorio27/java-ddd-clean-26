package com.sandbox.app.config;

import com.sandbox.application.usecase.CreateOrderUseCase;
import com.sandbox.application.usecase.GetOrderDetailsUseCase;
import com.sandbox.application.usecase.PayOrderUseCase;
import com.sandbox.application.port.UnitOfWork;
import com.sandbox.customers.domain.port.CustomerRepository;
import com.sandbox.orders.domain.port.OrderEventPublisher;
import com.sandbox.orders.domain.port.OrderRepository;
import com.sandbox.orders.domain.service.OrderDomainService;
import com.sandbox.payments.domain.port.PaymentGateway;
import com.sandbox.payments.domain.port.PaymentRepository;
import com.sandbox.shared.kernel.money.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class ApplicationConfig {

    @Bean
    OrderDomainService orderDomainService() {
        return new OrderDomainService(Money.of(new BigDecimal("10000"), "USD"));
    }

    @Bean
    CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository,
                                          CustomerRepository customerRepository,
                                          OrderEventPublisher eventPublisher,
                                          OrderDomainService orderDomainService,
                                          UnitOfWork unitOfWork) {
        return new CreateOrderUseCase(orderRepository, customerRepository, eventPublisher,
                orderDomainService, unitOfWork);
    }

    @Bean
    GetOrderDetailsUseCase getOrderDetailsUseCase(OrderRepository orderRepository,
                                                  CustomerRepository customerRepository,
                                                  PaymentRepository paymentRepository) {
        return new GetOrderDetailsUseCase(orderRepository, customerRepository, paymentRepository);
    }

    @Bean
    PayOrderUseCase payOrderUseCase(OrderRepository orderRepository,
                                    PaymentRepository paymentRepository,
                                    PaymentGateway paymentGateway,
                                    UnitOfWork unitOfWork) {
        return new PayOrderUseCase(orderRepository, paymentRepository, paymentGateway, unitOfWork);
    }
}
