package com.sandbox.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sandbox")
@EntityScan(basePackages = "com.sandbox.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.sandbox.infrastructure.persistence")
public class SandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandboxApplication.class, args);
    }
}
