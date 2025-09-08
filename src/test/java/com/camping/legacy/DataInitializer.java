package com.camping.legacy;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataInitializer implements InitializingBean {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Override
    public void afterPropertiesSet() {
        List<String> tableNames = entityManager.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                .map(this::getTableName)
                .toList();

        databaseCleanup.setTableNames(tableNames);
    }

    private String getTableName(EntityType<?> e) {
        Table table = e.getJavaType().getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return e.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Transactional
    public void execute() {
        databaseCleanup.execute();

        loadInitialData();
    }

    private void loadInitialData() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data.sql");
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data.sql", e);
        }
    }
}

