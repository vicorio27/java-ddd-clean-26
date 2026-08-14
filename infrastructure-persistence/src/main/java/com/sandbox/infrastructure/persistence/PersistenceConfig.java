package com.sandbox.infrastructure.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EntityScan(basePackages = "com.sandbox.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.sandbox.infrastructure.persistence")
@EnableTransactionManagement
public class PersistenceConfig {
}
