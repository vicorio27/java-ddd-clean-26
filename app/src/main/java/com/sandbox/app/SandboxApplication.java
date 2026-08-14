package com.sandbox.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Composition root. El wiring de JPA vive en el modulo que lo necesita
 * ({@code infrastructure-persistence}), no aqui: el app module no debe
 * conocer las clases de persistencia.
 */
@SpringBootApplication(scanBasePackages = "com.sandbox")
@EnableScheduling
public class SandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandboxApplication.class, args);
    }
}
