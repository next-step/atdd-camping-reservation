package com.camping.legacy.domain;

import com.camping.legacy.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Campsite 도메인 단위 테스트")
class CampsiteTest {

    @Nested
    @DisplayName("예약 가용성 확인 테스트")
    class AvailabilityTest {

        @Test
        @DisplayName("예약이 없는 기간은 예약 가능하다")
        void isAvailableForPeriod_WithNoReservations_ReturnsTrue() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(7);

            // When
            boolean available = campsite.isAvailableForPeriod(startDate, endDate);

            // Then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("활성 예약이 있는 기간은 예약 불가능하다")
        void isAvailableForPeriod_WithActiveReservation_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation existingReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    campsite);
            existingReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(existingReservation);

            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(12);

            // When
            boolean available = campsite.isAvailableForPeriod(startDate, endDate);

            // Then
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("취소된 예약이 있는 기간은 예약 가능하다")
        void isAvailableForPeriod_WithCancelledReservation_ReturnsTrue() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation cancelledReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    campsite);
            cancelledReservation.setStatus(ReservationStatus.CANCELLED);
            campsite.getReservations().add(cancelledReservation);

            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(12);

            // When
            boolean available = campsite.isAvailableForPeriod(startDate, endDate);

            // Then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("겹치지 않는 예약이 있어도 예약 가능하다")
        void isAvailableForPeriod_WithNonOverlappingReservation_ReturnsTrue() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation existingReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(1), 
                    LocalDate.now().plusDays(3), 
                    campsite);
            existingReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(existingReservation);

            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(7);

            // When
            boolean available = campsite.isAvailableForPeriod(startDate, endDate);

            // Then
            assertThat(available).isTrue();
        }
    }

    @Nested
    @DisplayName("예약 가용성 검증 테스트")
    class ValidateAvailabilityTest {

        @Test
        @DisplayName("예약 가능한 기간에 대해서는 예외가 발생하지 않는다")
        void validateAvailabilityForPeriod_WithAvailablePeriod_DoesNotThrowException() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(7);

            // When & Then (예외가 발생하지 않으면 성공)
            campsite.validateAvailabilityForPeriod(startDate, endDate);
        }

        @Test
        @DisplayName("예약 불가능한 기간에 대해서는 ConflictException이 발생한다")
        void validateAvailabilityForPeriod_WithUnavailablePeriod_ThrowsConflictException() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation existingReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    campsite);
            existingReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(existingReservation);

            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(12);

            // When & Then
            assertThatThrownBy(() -> campsite.validateAvailabilityForPeriod(startDate, endDate))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("해당 기간에 이미 예약이 존재합니다");
        }
    }

    @Nested
    @DisplayName("특정 날짜 예약 상태 확인 테스트")
    class ReservationStatusTest {

        @Test
        @DisplayName("예약이 없는 날짜는 예약되지 않은 상태다")
        void isReservedOn_WithNoReservations_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            LocalDate checkDate = LocalDate.now().plusDays(5);

            // When
            boolean reserved = campsite.isReservedOn(checkDate);

            // Then
            assertThat(reserved).isFalse();
        }

        @Test
        @DisplayName("활성 예약이 있는 날짜는 예약된 상태다")
        void isReservedOn_WithActiveReservation_ReturnsTrue() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation activeReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    campsite);
            activeReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(activeReservation);

            LocalDate checkDate = LocalDate.now().plusDays(7);

            // When
            boolean reserved = campsite.isReservedOn(checkDate);

            // Then
            assertThat(reserved).isTrue();
        }

        @Test
        @DisplayName("취소된 예약이 있는 날짜는 예약되지 않은 상태다")
        void isReservedOn_WithCancelledReservation_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation cancelledReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    campsite);
            cancelledReservation.setStatus(ReservationStatus.CANCELLED);
            campsite.getReservations().add(cancelledReservation);

            LocalDate checkDate = LocalDate.now().plusDays(7);

            // When
            boolean reserved = campsite.isReservedOn(checkDate);

            // Then
            assertThat(reserved).isFalse();
        }

        @Test
        @DisplayName("예약 기간 밖의 날짜는 예약되지 않은 상태다")
        void isReservedOn_WithDateOutsideReservationPeriod_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation activeReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    campsite);
            activeReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(activeReservation);

            LocalDate checkDate = LocalDate.now().plusDays(15);

            // When
            boolean reserved = campsite.isReservedOn(checkDate);

            // Then
            assertThat(reserved).isFalse();
        }
    }

    @Nested
    @DisplayName("사이트 크기 분류 테스트")
    class SiteSizeTest {

        @Test
        @DisplayName("A로 시작하는 사이트는 대형 사이트다")
        void isLargeSite_WithASiteNumber_ReturnsTrue() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 6);

            // When
            boolean isLarge = campsite.isLargeSite();

            // Then
            assertThat(isLarge).isTrue();
        }

        @Test
        @DisplayName("B로 시작하는 사이트는 대형 사이트가 아니다")
        void isLargeSite_WithBSiteNumber_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite("B001", "소형 사이트", 2);

            // When
            boolean isLarge = campsite.isLargeSite();

            // Then
            assertThat(isLarge).isFalse();
        }

        @Test
        @DisplayName("null 사이트 번호는 대형 사이트가 아니다")
        void isLargeSite_WithNullSiteNumber_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite(null, "사이트", 4);

            // When
            boolean isLarge = campsite.isLargeSite();

            // Then
            assertThat(isLarge).isFalse();
        }

        @Test
        @DisplayName("B로 시작하는 사이트는 소형 사이트다")
        void isSmallSite_WithBSiteNumber_ReturnsTrue() {
            // Given
            Campsite campsite = new Campsite("B001", "소형 사이트", 2);

            // When
            boolean isSmall = campsite.isSmallSite();

            // Then
            assertThat(isSmall).isTrue();
        }

        @Test
        @DisplayName("A로 시작하는 사이트는 소형 사이트가 아니다")
        void isSmallSite_WithASiteNumber_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 6);

            // When
            boolean isSmall = campsite.isSmallSite();

            // Then
            assertThat(isSmall).isFalse();
        }

        @Test
        @DisplayName("null 사이트 번호는 소형 사이트가 아니다")
        void isSmallSite_WithNullSiteNumber_ReturnsFalse() {
            // Given
            Campsite campsite = new Campsite(null, "사이트", 4);

            // When
            boolean isSmall = campsite.isSmallSite();

            // Then
            assertThat(isSmall).isFalse();
        }
    }

    @Nested
    @DisplayName("기간별 활성 예약 조회 테스트")
    class ActiveReservationsTest {

        @Test
        @DisplayName("활성 예약이 없으면 빈 리스트를 반환한다")
        void getActiveReservationsInPeriod_WithNoReservations_ReturnsEmptyList() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(10);

            // When
            List<Reservation> activeReservations = campsite.getActiveReservationsInPeriod(startDate, endDate);

            // Then
            assertThat(activeReservations).isEmpty();
        }

        @Test
        @DisplayName("기간 내 활성 예약만 반환한다")
        void getActiveReservationsInPeriod_WithMixedReservations_ReturnsOnlyActive() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);

            // 활성 예약 (기간 내)
            Reservation activeReservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(7), 
                    LocalDate.now().plusDays(12), 
                    campsite);
            activeReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(activeReservation);

            // 취소된 예약 (기간 내)
            Reservation cancelledReservation = new Reservation("이영희", 
                    LocalDate.now().plusDays(8), 
                    LocalDate.now().plusDays(13), 
                    campsite);
            cancelledReservation.setStatus(ReservationStatus.CANCELLED);
            campsite.getReservations().add(cancelledReservation);

            // 활성 예약 (기간 밖)
            Reservation outsideReservation = new Reservation("박민수", 
                    LocalDate.now().plusDays(20), 
                    LocalDate.now().plusDays(25), 
                    campsite);
            outsideReservation.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(outsideReservation);

            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(15);

            // When
            List<Reservation> activeReservations = campsite.getActiveReservationsInPeriod(startDate, endDate);

            // Then
            assertThat(activeReservations).hasSize(1);
            assertThat(activeReservations.get(0)).isEqualTo(activeReservation);
        }

        @Test
        @DisplayName("기간과 겹치는 활성 예약들을 모두 반환한다")
        void getActiveReservationsInPeriod_WithMultipleActiveReservations_ReturnsAll() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);

            // 첫 번째 활성 예약
            Reservation reservation1 = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(8), 
                    campsite);
            reservation1.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(reservation1);

            // 두 번째 활성 예약
            Reservation reservation2 = new Reservation("이영희", 
                    LocalDate.now().plusDays(10), 
                    LocalDate.now().plusDays(15), 
                    campsite);
            reservation2.setStatus(ReservationStatus.CONFIRMED);
            campsite.getReservations().add(reservation2);

            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(12);

            // When
            List<Reservation> activeReservations = campsite.getActiveReservationsInPeriod(startDate, endDate);

            // Then
            assertThat(activeReservations).hasSize(2);
            assertThat(activeReservations).containsExactlyInAnyOrder(reservation1, reservation2);
        }
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("모든 매개변수로 캠프사이트를 생성한다")
        void constructor_WithAllParameters_CreatesSuccessfully() {
            // Given & When
            Campsite campsite = new Campsite("A001", "대형 사이트", 6);

            // Then
            assertThat(campsite.getSiteNumber()).isEqualTo("A001");
            assertThat(campsite.getDescription()).isEqualTo("대형 사이트");
            assertThat(campsite.getMaxPeople()).isEqualTo(6);
            assertThat(campsite.getReservations()).isEmpty();
        }

        @Test
        @DisplayName("기본 생성자로 캠프사이트를 생성한다")
        void defaultConstructor_CreatesSuccessfully() {
            // Given & When
            Campsite campsite = new Campsite();

            // Then
            assertThat(campsite.getSiteNumber()).isNull();
            assertThat(campsite.getDescription()).isNull();
            assertThat(campsite.getMaxPeople()).isNull();
            assertThat(campsite.getReservations()).isEmpty();
        }
    }
}
