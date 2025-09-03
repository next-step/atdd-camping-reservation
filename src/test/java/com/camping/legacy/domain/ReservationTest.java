package com.camping.legacy.domain;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Reservation 도메인 단위 테스트")
class ReservationTest {

    @Nested
    @DisplayName("팩토리 메서드 테스트")
    class CreateTest {

        @Test
        @DisplayName("올바른 예약 요청으로 예약을 생성하면 성공한다")
        void createReservation_WithValidRequest_Success() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            ReservationRequest request = new ReservationRequest(
                    "김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7),
                    "A001", 
                    "010-1234-5678",
                    null, null, null
            );

            // When
            Reservation reservation = Reservation.create(request, campsite);

            // Then
            assertThat(reservation.getCustomerName()).isEqualTo("김철수");
            assertThat(reservation.getStartDate()).isEqualTo(LocalDate.now().plusDays(5));
            assertThat(reservation.getEndDate()).isEqualTo(LocalDate.now().plusDays(7));
            assertThat(reservation.getCampsite()).isEqualTo(campsite);
            assertThat(reservation.getPhoneNumber()).isEqualTo("010-1234-5678");
            assertThat(reservation.getConfirmationCode()).hasSize(6);
        }

        @Test
        @DisplayName("null 고객명으로 예약 생성 시 예외가 발생한다")
        void createReservation_WithNullCustomerName_ThrowsException() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            ReservationRequest request = new ReservationRequest(
                    null, 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7),
                    "A001", 
                    "010-1234-5678",
                    null, null, null
            );

            // When & Then
            assertThatThrownBy(() -> Reservation.create(request, campsite))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("예약자 이름을 입력해주세요");
        }

        @Test
        @DisplayName("빈 고객명으로 예약 생성 시 예외가 발생한다")
        void createReservation_WithEmptyCustomerName_ThrowsException() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            ReservationRequest request = new ReservationRequest(
                    "  ", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7),
                    "A001", 
                    "010-1234-5678",
                    null, null, null
            );

            // When & Then
            assertThatThrownBy(() -> Reservation.create(request, campsite))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("예약자 이름을 입력해주세요");
        }

        @Test
        @DisplayName("null 전화번호로 예약 생성 시 예외가 발생한다")
        void createReservation_WithNullPhoneNumber_ThrowsException() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            ReservationRequest request = new ReservationRequest(
                    "김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7),
                    "A001", 
                    null,
                    null, null, null
            );

            // When & Then
            assertThatThrownBy(() -> Reservation.create(request, campsite))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("전화번호를 입력해주세요");
        }
    }

    @Nested
    @DisplayName("예약 기간 유효성 검증 테스트")
    class ValidateReservationPeriodTest {

        @Test
        @DisplayName("올바른 예약 기간은 검증을 통과한다")
        void validateReservationPeriod_WithValidDates_Success() {
            // Given
            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(7);

            // When & Then (예외가 발생하지 않으면 성공)
            Reservation.validateReservationPeriod(startDate, endDate);
        }

        @Test
        @DisplayName("null 시작일로 검증 시 예외가 발생한다")
        void validateReservationPeriod_WithNullStartDate_ThrowsException() {
            // Given
            LocalDate endDate = LocalDate.now().plusDays(7);

            // When & Then
            assertThatThrownBy(() -> Reservation.validateReservationPeriod(null, endDate))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("예약 기간을 선택해주세요");
        }

        @Test
        @DisplayName("null 종료일로 검증 시 예외가 발생한다")
        void validateReservationPeriod_WithNullEndDate_ThrowsException() {
            // Given
            LocalDate startDate = LocalDate.now().plusDays(5);

            // When & Then
            assertThatThrownBy(() -> Reservation.validateReservationPeriod(startDate, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("예약 기간을 선택해주세요");
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전이면 예외가 발생한다")
        void validateReservationPeriod_WithEndDateBeforeStartDate_ThrowsException() {
            // Given
            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(5);

            // When & Then
            assertThatThrownBy(() -> Reservation.validateReservationPeriod(startDate, endDate))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("종료일이 시작일보다 이전일 수 없습니다");
        }

        @Test
        @DisplayName("과거 날짜로 예약 시 예외가 발생한다")
        void validateReservationPeriod_WithPastDate_ThrowsException() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(1);

            // When & Then
            assertThatThrownBy(() -> Reservation.validateReservationPeriod(startDate, endDate))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("과거 날짜로 예약할 수 없습니다");
        }

        @Test
        @DisplayName("30일을 초과한 날짜로 예약 시 예외가 발생한다")
        void validateReservationPeriod_WithDateExceeding30Days_ThrowsException() {
            // Given
            LocalDate startDate = LocalDate.now().plusDays(31);
            LocalDate endDate = LocalDate.now().plusDays(33);

            // When & Then
            assertThatThrownBy(() -> Reservation.validateReservationPeriod(startDate, endDate))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("30일 이내 예약만 가능합니다");
        }
    }

    @Nested
    @DisplayName("확인 코드 검증 테스트")
    class ValidateConfirmationCodeTest {

        @Test
        @DisplayName("올바른 확인 코드는 검증을 통과한다")
        void validateConfirmationCode_WithCorrectCode_Success() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setConfirmationCode("ABC123");

            // When & Then (예외가 발생하지 않으면 성공)
            reservation.validateConfirmationCode("ABC123");
        }

        @Test
        @DisplayName("잘못된 확인 코드로 검증 시 예외가 발생한다")
        void validateConfirmationCode_WithWrongCode_ThrowsException() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setConfirmationCode("ABC123");

            // When & Then
            assertThatThrownBy(() -> reservation.validateConfirmationCode("WRONG1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("확인 코드가 일치하지 않습니다");
        }
    }

    @Nested
    @DisplayName("예약 취소 테스트")
    class CancelTest {

        @Test
        @DisplayName("미래 예약을 취소하면 CANCELLED 상태가 된다")
        void cancel_FutureReservation_SetsToCancelled() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));

            // When
            reservation.cancel();

            // Then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("당일 예약을 취소하면 CANCELLED_SAME_DAY 상태가 된다")
        void cancel_SameDayReservation_SetsToCancelledSameDay() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now(), 
                    LocalDate.now().plusDays(2), 
                    new Campsite("A001", "대형 사이트", 4));

            // When
            reservation.cancel();

            // Then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED_SAME_DAY);
        }
    }

    @Nested
    @DisplayName("예약 업데이트 테스트")
    class UpdateReservationTest {

        @Test
        @DisplayName("예약 정보를 업데이트하면 변경된다")
        void updateReservation_WithNewInfo_UpdatesSuccessfully() {
            // Given
            Campsite originalCampsite = new Campsite("A001", "대형 사이트", 4);
            Campsite newCampsite = new Campsite("A002", "대형 사이트", 6);
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    originalCampsite);
            reservation.setPhoneNumber("010-1234-5678");

            ReservationRequest updateRequest = new ReservationRequest(
                    "이영희", 
                    LocalDate.now().plusDays(10), 
                    LocalDate.now().plusDays(12),
                    "A002", 
                    "010-9876-5432",
                    null, null, null
            );

            // When
            reservation.updateReservation(updateRequest, newCampsite);

            // Then
            assertThat(reservation.getCustomerName()).isEqualTo("이영희");
            assertThat(reservation.getStartDate()).isEqualTo(LocalDate.now().plusDays(10));
            assertThat(reservation.getEndDate()).isEqualTo(LocalDate.now().plusDays(12));
            assertThat(reservation.getCampsite()).isEqualTo(newCampsite);
            assertThat(reservation.getPhoneNumber()).isEqualTo("010-9876-5432");
        }

        @Test
        @DisplayName("부분적으로만 업데이트하면 해당 필드만 변경된다")
        void updateReservation_WithPartialInfo_UpdatesOnlySpecifiedFields() {
            // Given
            Campsite campsite = new Campsite("A001", "대형 사이트", 4);
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    campsite);
            reservation.setPhoneNumber("010-1234-5678");

            ReservationRequest updateRequest = new ReservationRequest(
                    "이영희", 
                    null, 
                    null,
                    null, 
                    null,
                    null, null, null
            );

            // When
            reservation.updateReservation(updateRequest, null);

            // Then
            assertThat(reservation.getCustomerName()).isEqualTo("이영희");
            assertThat(reservation.getStartDate()).isEqualTo(LocalDate.now().plusDays(5)); // 변경되지 않음
            assertThat(reservation.getEndDate()).isEqualTo(LocalDate.now().plusDays(7)); // 변경되지 않음
            assertThat(reservation.getCampsite()).isEqualTo(campsite); // 변경되지 않음
            assertThat(reservation.getPhoneNumber()).isEqualTo("010-1234-5678"); // 변경되지 않음
        }
    }

    @Nested
    @DisplayName("날짜 범위 확인 테스트")
    class DateRangeTest {

        @Test
        @DisplayName("특정 날짜가 예약 기간 내에 있으면 true를 반환한다")
        void isWithinDateRange_WithDateInRange_ReturnsTrue() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));

            // When
            boolean result = reservation.isWithinDateRange(LocalDate.now().plusDays(6));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("특정 날짜가 예약 기간 밖에 있으면 false를 반환한다")
        void isWithinDateRange_WithDateOutOfRange_ReturnsFalse() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));

            // When
            boolean result = reservation.isWithinDateRange(LocalDate.now().plusDays(10));

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("기간이 겹치면 true를 반환한다")
        void overlaps_WithOverlappingPeriod_ReturnsTrue() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    new Campsite("A001", "대형 사이트", 4));

            // When
            boolean result = reservation.overlaps(
                    LocalDate.now().plusDays(7), 
                    LocalDate.now().plusDays(12)
            );

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("기간이 겹치지 않으면 false를 반환한다")
        void overlaps_WithNonOverlappingPeriod_ReturnsFalse() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(10), 
                    new Campsite("A001", "대형 사이트", 4));

            // When
            boolean result = reservation.overlaps(
                    LocalDate.now().plusDays(15), 
                    LocalDate.now().plusDays(20)
            );

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("고객 정보 매칭 테스트")
    class CustomerMatchingTest {

        @Test
        @DisplayName("고객 이름과 전화번호가 일치하면 true를 반환한다")
        void matchesCustomer_WithMatchingInfo_ReturnsTrue() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setPhoneNumber("010-1234-5678");

            // When
            boolean result = reservation.matchesCustomer("김철수", "010-1234-5678");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("고객 이름이 다르면 false를 반환한다")
        void matchesCustomer_WithDifferentName_ReturnsFalse() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setPhoneNumber("010-1234-5678");

            // When
            boolean result = reservation.matchesCustomer("이영희", "010-1234-5678");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("전화번호가 다르면 false를 반환한다")
        void matchesCustomer_WithDifferentPhone_ReturnsFalse() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setPhoneNumber("010-1234-5678");

            // When
            boolean result = reservation.matchesCustomer("김철수", "010-9999-9999");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("키워드 검색 매칭 테스트")
    class KeywordMatchingTest {

        @Test
        @DisplayName("고객 이름에 키워드가 포함되면 true를 반환한다")
        void matchesKeyword_WithKeywordInName_ReturnsTrue() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setPhoneNumber("010-1234-5678");

            // When
            boolean result = reservation.matchesKeyword("철수");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("전화번호에 키워드가 포함되면 true를 반환한다")
        void matchesKeyword_WithKeywordInPhone_ReturnsTrue() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setPhoneNumber("010-1234-5678");

            // When
            boolean result = reservation.matchesKeyword("1234");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("키워드가 어디에도 없으면 false를 반환한다")
        void matchesKeyword_WithNoMatchingKeyword_ReturnsFalse() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            reservation.setPhoneNumber("010-1234-5678");

            // When
            boolean result = reservation.matchesKeyword("영희");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("전화번호가 null이고 이름에 키워드가 없으면 false를 반환한다")
        void matchesKeyword_WithNullPhoneAndNoMatchInName_ReturnsFalse() {
            // Given
            Reservation reservation = new Reservation("김철수", 
                    LocalDate.now().plusDays(5), 
                    LocalDate.now().plusDays(7), 
                    new Campsite("A001", "대형 사이트", 4));
            // phoneNumber는 null

            // When
            boolean result = reservation.matchesKeyword("영희");

            // Then
            assertThat(result).isFalse();
        }
    }
}