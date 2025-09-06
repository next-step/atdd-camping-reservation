package com.camping.legacy.validator;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampsiteReservationValidatorTest {

    @Mock
    private ReservationRepository reservationRepository;

    private CampsiteReservationValidator validator;
    private Campsite campsite;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        validator = new CampsiteReservationValidator(reservationRepository);
        campsite = new Campsite("A-1", "테스트 캠핑장", 4);
        startDate = LocalDate.now().plusDays(10);
        endDate = LocalDate.now().plusDays(12);
    }

    @Test
    @DisplayName("예약 가능한 기간 - 예외 발생하지 않음")
    void 예약_가능한_기간_예외_발생하지_않음() {
        // given
        when(reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate))
                .thenReturn(Collections.emptyList());

        // when & then
        assertDoesNotThrow(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate));
    }

    @Test
    @DisplayName("취소된 예약만 있는 경우 - 예외 발생하지 않음")
    void 취소된_예약만_있는_경우_예외_발생하지_않음() {
        // given
        Reservation cancelledReservation = createReservation("김영희", startDate, endDate, "CANCELLED");
        
        when(reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate))
                .thenReturn(Arrays.asList(cancelledReservation));

        // when & then
        assertDoesNotThrow(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate));
    }

    @Test
    @DisplayName("확정된 예약이 있는 경우 - 예외 발생")
    void 확정된_예약이_있는_경우_예외_발생() {
        // given
        Reservation confirmedReservation = createReservation("김영희", startDate, endDate, "CONFIRMED");
        
        when(reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate))
                .thenReturn(Arrays.asList(confirmedReservation));

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
        
        when(reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate))
                .thenReturn(Arrays.asList(confirmedReservation, cancelledReservation));

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
        
        when(reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate))
                .thenReturn(Arrays.asList(confirmedReservation1, confirmedReservation2));

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
        
        when(reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate))
                .thenReturn(Arrays.asList(sameDayCancelledReservation));

        // when & then
        assertDoesNotThrow(() -> validator.validateCampsiteAvailability(campsite, startDate, endDate));
    }

    private Reservation createReservation(String customerName, LocalDate startDate, LocalDate endDate, String status) {
        // Reservation 생성자를 사용할 수 없으므로 Mock 또는 ReflectionTestUtils 사용
        // 여기서는 간단하게 Mock을 사용
        Reservation reservation = new Reservation(customerName, startDate, endDate, campsite, "010-1234-5678", "ABC123");
        
        // status 설정을 위해 리플렉션 사용 (실제로는 @PrePersist에서 설정되지만 테스트를 위해)
        try {
            java.lang.reflect.Field statusField = Reservation.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(reservation, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return reservation;
    }
}