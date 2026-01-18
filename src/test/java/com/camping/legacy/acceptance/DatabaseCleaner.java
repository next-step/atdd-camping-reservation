package com.camping.legacy.acceptance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void clear() {
        // reservations 테이블만 초기화 (campsites는 기준 데이터이므로 유지)
        entityManager.createNativeQuery("TRUNCATE TABLE reservations").executeUpdate();
    }
}
