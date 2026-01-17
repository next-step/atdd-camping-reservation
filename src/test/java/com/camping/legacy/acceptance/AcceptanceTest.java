package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort
    int port;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        // reservations 테이블만 초기화 (campsites는 기준 데이터이므로 유지)
        entityManager.createNativeQuery("TRUNCATE TABLE reservations").executeUpdate();
    }

    private String getTableName(EntityType<?> entity) {
        Class<?> javaType = entity.getJavaType();
        Table tableAnnotation = javaType.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // @Table이 없으면 엔티티 이름 사용
        return entity.getName();
    }

}
