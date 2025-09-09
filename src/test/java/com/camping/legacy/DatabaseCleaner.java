package com.camping.legacy;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleaner {

    @PersistenceContext
    private final EntityManager entityManager;

    public DatabaseCleaner(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void clean() {
        entityManager.clear();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        getTableNames().forEach(tableName -> {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        });
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    private List<String> getTableNames() {
        return entityManager.getMetamodel()
                .getEntities()
                .stream()
                .filter(entityType -> entityType.getJavaType().isAnnotationPresent(Entity.class))
                .map(entityType -> entityType.getJavaType().getAnnotation(Table.class).name())
                .toList();
    }
}
