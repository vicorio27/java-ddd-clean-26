package com.sandbox.infrastructure.persistence.customers;

import com.sandbox.customers.domain.model.Customer;
import com.sandbox.customers.domain.port.CustomerRepository;
import com.sandbox.shared.kernel.id.CustomerId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    public CustomerRepositoryAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        return toDomain(jpaRepository.save(toEntity(customer)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findById(CustomerId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    private CustomerJpaEntity toEntity(Customer customer) {
        var entity = new CustomerJpaEntity();
        entity.setId(customer.id().value());
        entity.setName(customer.name());
        entity.setEmail(customer.email());
        entity.setStatus(customer.status().name());
        return entity;
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        return Customer.reconstitute(new CustomerId(entity.getId()), entity.getName(), entity.getEmail(),
                Customer.CustomerStatus.valueOf(entity.getStatus()));
    }
}
