package com.camping.acceptance.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseCleanup(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void execute() {
        entityManager.flush();

        // FK 제약조건 비활성화
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        // 모든 테이블 TRUNCATE
        List<String> tableNames = entityManager
                .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")
                .getResultList();

        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }

        // FK 제약조건 재활성화
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }
}