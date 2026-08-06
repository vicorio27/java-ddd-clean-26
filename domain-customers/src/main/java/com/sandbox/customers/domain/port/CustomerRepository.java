package com.sandbox.customers.domain.port;

import com.sandbox.customers.domain.model.Customer;
import com.sandbox.shared.kernel.id.CustomerId;

import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);
}
