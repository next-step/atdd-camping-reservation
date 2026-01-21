package com.camping.legacy.repository;

import com.camping.legacy.CampingApplication;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 버그 재현 테스트: ReservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual
 *
 * 발견된 버그:
 * - 취소된 예약(CANCELLED)도 충돌로 판단하는 문제
 * - 경계 날짜 처리 문제 (체크아웃일 = 다음 체크인일)
 *
 * @see docs/버그.md
 */
@DataJpaTest
@ContextConfiguration(classes = CampingApplication.class)
@DisplayName("ReservationRepository 버그 재현 테스트")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CampsiteRepository campsiteRepository;

    private Campsite siteA1;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        siteA1 = campsiteRepository.findBySiteNumber("A-1").orElseThrow();
    }

    @Nested
    @DisplayName("버그: 취소된 예약 충돌 판단")
    class CancelledReservationConflictBug {

        @Test
        @DisplayName("[BUG] 취소된 예약은 충돌 검사에서 제외되어야 함")
        void 취소된_예약은_충돌로_판단하면_안됨() {
            // given - 취소된 예약이 존재한다
            Reservation cancelledReservation = createReservation(
                    siteA1,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(12),
                    "CANCELLED"
            );
            reservationRepository.save(cancelledReservation);

            // when - 같은 기간에 충돌 여부를 확인한다
            boolean hasConflict = reservationRepository
                    .existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            siteA1,
                            LocalDate.now().plusDays(12),  // endDate
                            LocalDate.now().plusDays(10)   // startDate
                    );

            // then - 취소된 예약은 충돌로 판단하면 안 됨
            assertThat(hasConflict)
                    .as("취소된 예약(CANCELLED)은 충돌 검사에서 제외되어야 합니다")
                    .isFalse();  // 현재 버그: true 반환 예상
        }
    }

    @Nested
    @DisplayName("버그: 경계 날짜 처리")
    class BoundaryDateBug {

        @Test
        @DisplayName("[BUG] 체크아웃일에 새 예약 시작 가능해야 함")
        void 체크아웃일에_체크인_가능해야_함() {
            // given - 기존 예약이 N+10 ~ N+12 에 존재한다
            Reservation existingReservation = createReservation(
                    siteA1,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(12),
                    "CONFIRMED"
            );
            reservationRepository.save(existingReservation);

            // when - 기존 예약 종료일(N+12)에 시작하는 새 예약 충돌 여부 확인
            boolean hasConflict = reservationRepository
                    .existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            siteA1,
                            LocalDate.now().plusDays(14),  // newEndDate
                            LocalDate.now().plusDays(12)   // newStartDate (= 기존 종료일)
                    );

            // then - 체크아웃 당일에 새 고객 체크인 가능해야 함
            assertThat(hasConflict)
                    .as("체크아웃일(N+12)에 새 예약 시작(N+12)이 가능해야 합니다")
                    .isFalse();  // 현재 버그: true 반환 예상
        }
    }

    private Reservation createReservation(Campsite campsite, LocalDate startDate,
                                          LocalDate endDate, String status) {
        Reservation reservation = new Reservation();
        reservation.setCampsite(campsite);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setStatus(status);
        reservation.setCustomerName("테스트고객");
        reservation.setPhoneNumber("010-1234-5678");
        reservation.setConfirmationCode("TEST01");
        return reservation;
    }
}