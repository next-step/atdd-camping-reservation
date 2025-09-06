package com.camping.legacy.validator;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CampsiteReservationValidatorTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CampsiteRepository campsiteRepository;

    private CampsiteReservationValidator validator;
    private Campsite campsite;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        validator = new CampsiteReservationValidator(reservationRepository);

        campsite = new Campsite("D-1", "테스트 캠핑장", 4);
        campsite = campsiteRepository.save(campsite);
        
        startDate = LocalDate.now().plusDays(10);
        endDate = LocalDate.now().plusDays(12);
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("예약 가능한 기간 - 예외 발생하지 않음")
    void 예약_가능한_기간_예외_발생하지_않음() {
        // when & then
        assertDoesNotThrow(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate));
    }

    @Test
    @DisplayName("취소된 예약만 있는 경우 - 예외 발생하지 않음")
    void 취소된_예약만_있는_경우_예외_발생하지_않음() {
        // given
        Reservation cancelledReservation = createReservation("김영희", startDate, endDate, "CANCELLED");
        reservationRepository.save(cancelledReservation);

        // when & then
        assertDoesNotThrow(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate));
    }

    @Test
    @DisplayName("확정된 예약이 있는 경우 - 예외 발생")
    void 확정된_예약이_있는_경우_예외_발생() {
        // given
        Reservation confirmedReservation = createReservation("김영희", startDate, endDate, "CONFIRMED");
        reservationRepository.save(confirmedReservation);

        // when & then
        assertThatThrownBy(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("확정된 예약과 취소된 예약이 모두 있는 경우 - 예외 발생")
    void 확정된_예약과_취소된_예약이_모두_있는_경우_예외_발생() {
        // given
        Reservation confirmedReservation = createReservation("김영희", startDate, endDate, "CONFIRMED");
        Reservation cancelledReservation = createReservation("박철수", startDate, endDate, "CANCELLED");
        reservationRepository.save(confirmedReservation);
        reservationRepository.save(cancelledReservation);

        // when & then
        assertThatThrownBy(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("여러 확정된 예약이 있는 경우 - 예외 발생")
    void 여러_확정된_예약이_있는_경우_예외_발생() {
        // given
        Reservation confirmedReservation1 = createReservation("김영희", startDate, endDate, "CONFIRMED");
        Reservation confirmedReservation2 = createReservation("박철수", startDate.plusDays(1), endDate.plusDays(1), "CONFIRMED");
        reservationRepository.save(confirmedReservation1);
        reservationRepository.save(confirmedReservation2);

        // when & then
        assertThatThrownBy(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("당일 취소된 예약만 있는 경우 - 예외 발생하지 않음")
    void 당일_취소된_예약만_있는_경우_예외_발생하지_않음() {
        // given
        Reservation sameDayCancelledReservation = createReservation("김영희", startDate, endDate, "CANCELLED_SAME_DAY");
        reservationRepository.save(sameDayCancelledReservation);

        // when & then
        assertDoesNotThrow(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate));
    }

    private Reservation createReservation(String customerName, LocalDate startDate, LocalDate endDate, String status) {
        // Reservation 생성자 사용
        Reservation reservation = new Reservation(customerName, startDate, endDate, campsite, "010-1234-5678", "ABC123");
        
        // status 설정을 위해 Spring의 ReflectionTestUtils 사용
        ReflectionTestUtils.setField(reservation, "status", status);
        
        return reservation;
    }
}