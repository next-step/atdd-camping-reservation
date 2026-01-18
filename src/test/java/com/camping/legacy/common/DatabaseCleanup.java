package com.camping.legacy.common;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @Transactional
    public void execute() {
        entityManager.flush();

        // 외래키 제약 조건 비활성화하여 삭제 순서 무시
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        for (String tableName : getTableNames()) {
            // 테이블 데이터 전체 삭제
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            // ID 자동증가값을 1부터 다시 시작
            entityManager.createNativeQuery(
                    "ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1"
            ).executeUpdate();
        }
        // 외래키 제약조건 다시 활성화
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

        // 기본 사이트 데이터 복원
        insertDefaultSites();
    }

    private void insertDefaultSites() {
        entityManager.createNativeQuery(
                "INSERT INTO campsites (site_number, description, max_people) VALUES " +
                        "('A-1', '대형 사이트 - 전기 있음', 6), " +
                        "('A-2', '대형 사이트 - 전기 있음', 6), " +
                        "('A-3', '대형 사이트 - 전기 있음', 6), " +
                        "('B-1', '소형 사이트 - 전기 있음', 4), " +
                        "('B-2', '소형 사이트 - 전기 있음', 4)"
        ).executeUpdate();
    }

    private List<String> getTableNames() {
        if (tableNames == null) {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                    .map(this::getTableName)
                    .collect(Collectors.toList());
        }
        return tableNames;
    }

    private String getTableName(EntityType<?> entity) {
        Table tableAnnotation = entity.getJavaType().getAnnotation(Table.class);
        return tableAnnotation != null ? tableAnnotation.name() : entity.getName();
    }
}