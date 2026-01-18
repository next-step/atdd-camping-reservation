package com.camping.legacy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void execute() {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE reservations RESTART IDENTITY").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE campsites RESTART IDENTITY").executeUpdate();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }
}
