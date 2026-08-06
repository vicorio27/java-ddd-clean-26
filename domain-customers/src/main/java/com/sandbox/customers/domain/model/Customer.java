package com.sandbox.customers.domain.model;

import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.id.CustomerId;

public final class Customer {

    private final CustomerId id;
    private final String name;
    private final String email;
    private CustomerStatus status;

    private Customer(CustomerId id, String name, String email, CustomerStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    public static Customer register(String name, String email) {
        if (email == null || !email.contains("@")) {
            throw new DomainException("Invalid customer email");
        }
        return new Customer(CustomerId.newId(), name, email, CustomerStatus.ACTIVE);
    }

    public static Customer reconstitute(CustomerId id, String name, String email, CustomerStatus status) {
        return new Customer(id, name, email, status);
    }

    public void suspend() {
        this.status = CustomerStatus.SUSPENDED;
    }

    public boolean canPlaceOrders() {
        return status == CustomerStatus.ACTIVE;
    }

    public CustomerId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public CustomerStatus status() {
        return status;
    }

    public enum CustomerStatus {
        ACTIVE, SUSPENDED
    }
}
