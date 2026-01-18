package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Test
    @DisplayName("자신(=id)을 제외하고 해당 기간에 겹치는 예약이 있는지 확인한다")
    void checkOverlapExcludingSelf() {
        // Given
        Campsite startCampsite = campsiteRepository.save(new Campsite("TEST-A-1", "Description", 5));

        LocalDate today = LocalDate.now();
        // 기존 예약 (오늘 ~ 2일 뒤)
        Reservation p1 = new Reservation();
        p1.setCampsite(startCampsite);
        p1.setCustomerName("User1");
        p1.setStartDate(today);
        p1.setEndDate(today.plusDays(2));
        p1.setConfirmationCode("CODE1");
        Reservation saved = reservationRepository.save(p1);

        // When & Then

        // 1. 자신의 ID로 체크 -> 자신의 기간과 겹치더라도 자신을 제외하므로 false여야 함
        boolean overlapSelf = reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                startCampsite,
                today.plusDays(2), // 체크하려는 기간의 End
                today,             // 체크하려는 기간의 Start
                saved.getId()      // 제외할 ID (자신)
        );
        assertThat(overlapSelf).isFalse();

        // 2. 다른 ID(없는 ID)로 체크 -> 기존 예약과 겹치므로 true여야 함
        boolean overlapOther = reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                startCampsite,
                today.plusDays(1),
                today,
                saved.getId() + 999L // 다른 ID
        );
        assertThat(overlapOther).isTrue();

        // 3. 겹치지 않는 기간 체크 -> false
        boolean noOverlap = reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                startCampsite,
                today.plusDays(5),
                today.plusDays(3),
                saved.getId() + 999L
        );
        assertThat(noOverlap).isFalse();
    }
}
