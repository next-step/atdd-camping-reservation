package com.camping.legacy.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DB 초기화 유틸리티
 *
 * - 테스트 간 데이터 격리를 위해 모든 테이블 TRUNCATE(ID 시퀀스도 1부터 다시 시작)
 */
@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @SuppressWarnings("unchecked")
    private void initTableNames() {
        if (tableNames == null) {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .map(entity -> {
                        Table tableAnnotation = entity.getJavaType().getAnnotation(Table.class);
                        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                            return tableAnnotation.name();
                        }
                        // @Table 어노테이션이 없거나 name이 비어있으면 엔티티 이름을 스네이크 케이스로 변환
                        return camelToSnake(entity.getName());
                    })
                    .collect(Collectors.toList());
        }
    }

    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Transactional
    public void clear() {
        initTableNames();
        entityManager.flush();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH 1").executeUpdate();
        }

        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }
}
