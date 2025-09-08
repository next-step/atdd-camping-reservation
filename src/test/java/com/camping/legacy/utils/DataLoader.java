package com.camping.legacy.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataLoader {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void loadData() {
        em.createNativeQuery("DELETE FROM reservations").executeUpdate();
        em.createNativeQuery("DELETE FROM campsites").executeUpdate();

        em.createNativeQuery(
                "INSERT INTO campsites (site_number, description, max_people) VALUES " +
                        "('A-1', '대형 사이트 - 전기 있음, 화장실 인근', 6), " +
                        "('A-2', '대형 사이트 - 전기 있음, 개수대 인근', 6), " +
                        "('A-3', '대형 사이트 - 전기 있음, 계곡 전망', 6), " +
                        "('B-1', '소형 사이트 - 전기 있음, 매점 인근', 4), " +
                        "('B-2', '소형 사이트 - 전기 있음, 주차장 인근', 4), " +
                        "('B-3', '소형 사이트 - 전기 있음, 샤워장 인근', 4)"
        ).executeUpdate();

        final Long campsiteId = (Long) em.createNativeQuery("SELECT id FROM campsites WHERE site_number = 'A-1'")
                .getSingleResult();

        em.createNativeQuery(
                "INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) " +
                        "VALUES ('홍길동', DATEADD('DAY', 7, CURRENT_DATE), DATEADD('DAY', 9, CURRENT_DATE), DATEADD('DAY', 7, CURRENT_DATE), ?, " +
                        "'010-1234-5678', 'CONFIRMED', 'ABC123', CURRENT_TIMESTAMP)"
        ).setParameter(1, campsiteId).executeUpdate();
    }
}
