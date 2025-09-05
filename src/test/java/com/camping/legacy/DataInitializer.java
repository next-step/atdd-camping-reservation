package com.camping.legacy;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataInitializer {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ResourceLoader resourceLoader;

    @Transactional
    public void execute() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data.sql");
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data.sql", e);
        }
    }
}
