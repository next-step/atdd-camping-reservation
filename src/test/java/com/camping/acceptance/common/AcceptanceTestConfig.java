package com.camping.acceptance.common;

import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = "com.camping.acceptance")
public class AcceptanceTestConfig {

    @Bean
    public DatabaseCleanup databaseCleanup(EntityManager entityManager) {
        return new DatabaseCleanup(entityManager);
    }
}
