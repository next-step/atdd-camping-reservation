package com.camping.legacy.acceptance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void execute() {
        entityManager.flush();

        // H2 DB의 FK 체크 비활성화
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        // INFORMATION_SCHEMA에서 PUBLIC 스키마의 모든 테이블 목록 조회
        List<String> tableNames = getTableNames();

        // 각 테이블 TRUNCATE
        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }

        // FK 체크 다시 활성화
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private List<String> getTableNames() {
        return entityManager.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'"
        ).getResultList();
    }
}