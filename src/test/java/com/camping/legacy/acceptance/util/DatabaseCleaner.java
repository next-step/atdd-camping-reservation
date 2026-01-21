package com.camping.legacy.acceptance.util;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @PostConstruct
    public void init() {
        tableNames = entityManager.getMetamodel().getEntities().stream()
            .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
            .map(e -> {
                Table table = e.getJavaType().getAnnotation(Table.class);
                return table != null ? table.name() : convertToSnakeCase(e.getName());
            })
            .toList();
    }

    @Transactional
    public void clear() {
        entityManager.flush();
        entityManager.createNativeQuery("TRUNCATE TABLE reservations").executeUpdate();
    }

    private String convertToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
