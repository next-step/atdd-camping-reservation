package com.camping.legacy.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("test")
@Component
public class DatabaseCleanup implements InitializingBean {

    @PersistenceContext
    private EntityManager em;

    private List<String> tableNames;

    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() {
        tableNames = (List<String>) em.createNativeQuery("""
            
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = SCHEMA()
              AND TABLE_TYPE = 'TABLE'
        """).getResultList();
    }

    @Transactional
    public void execute() {
        em.flush();
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        for (String table : tableNames) {
            em.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
        }
        for (String table : tableNames) {
            try {
                em.createNativeQuery("ALTER TABLE " + table + " ALTER COLUMN ID RESTART WITH 1").executeUpdate();
            } catch (Exception ignore) {}
        }
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }
}
