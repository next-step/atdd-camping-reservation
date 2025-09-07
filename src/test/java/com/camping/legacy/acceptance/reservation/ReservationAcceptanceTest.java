package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTest;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceStep.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.util.ConcurrencyTestHelper;

public class ReservationAcceptanceTest extends AcceptanceTest {
    @DisplayName("예약 생성 - 성공")
    @Test
    void createReservation() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().build();

        ReservationResponse response = 예약_생성_성공(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo(request.getCustomerName());
        assertThat(response.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
        assertThat(response.getSiteNumber()).isEqualTo(request.getSiteNumber());
        assertThat(response.getConfirmationCode()).isNotNull();
    }

    @DisplayName("예약 생성 - 30일 초과 예약 실패")
    @Test
    void createReservationFailWithOver30Days() {
        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = LocalDate.now().plusDays(32);
        ReservationRequest request = new ReservationRequestTestDataBuilder().withDates(startDate, endDate).build();

        예약_생성_실패(request, 409, "예약일이 오늘 기준 30일을 초과할 수 없습니다.");
    }

    @DisplayName("예약 생성 - 과거 날짜 예약 실패")
    @Test
    void createReservationFailWithPastDate() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        ReservationRequest request = new ReservationRequestTestDataBuilder().withDates(startDate, endDate).build();

        예약_생성_실패(request, 409, "예약일이 과거일 수 없습니다.");
    }

    @DisplayName("예약 생성 - 종료일이 시작일 이전인 예약 실패")
    @Test
    void createReservationFailWithEndDateBeforeStartDate() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().minusDays(1);
        ReservationRequest request = new ReservationRequestTestDataBuilder().withDates(startDate, endDate).build();

        예약_생성_실패(request, 409, "종료일이 시작일보다 이전일 수 없습니다.");
    }

    @DisplayName("예약 생성 - 예약자 이름 null인 경우 실패")
    @Test
    void createReservationFailWithNullCustomerName() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withCustomerName(null).build();

        예약_생성_실패(request, 409, "예약자 이름을 입력해주세요.");
    }

    @DisplayName("예약 생성 - 예약자 이름 빈 문자열인 경우 실패")
    @Test
    void createReservationFailWithEmptyCustomerName() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withCustomerName("").build();

        예약_생성_실패(request, 409, "예약자 이름을 입력해주세요.");
    }

    @DisplayName("예약 생성 - 예약자 이름 공백만 있는 경우 실패")
    @Test
    void createReservationFailWithBlankCustomerName() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withCustomerName("   ").build();

        예약_생성_실패(request, 409, "예약자 이름을 입력해주세요.");
    }

    @DisplayName("예약 생성 - 전화번호 null인 경우 실패")
    @Test
    void createReservationFailWithNullPhoneNumber() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withPhoneNumber(null).build();

        예약_생성_실패(request, 409, "전화번호를 입력해주세요.");
    }

    @DisplayName("예약 생성 - 전화번호 빈 문자열인 경우 실패")
    @Test
    void createReservationFailWithEmptyPhoneNumber() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withPhoneNumber("").build();

        예약_생성_실패(request, 409, "전화번호를 입력해주세요.");
    }

    @DisplayName("예약 생성 - 전화번호 공백만 있는 경우 실패")
    @Test
    void createReservationFailWithBlankPhoneNumber() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withPhoneNumber("   ").build();

        예약_생성_실패(request, 409, "전화번호를 입력해주세요.");
    }

    @DisplayName("예약 생성 - 전화번호 형식이 잘못된 경우 실패")
    @Test
    void createReservationFailWithInvalidPhoneNumberFormat() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withPhoneNumber("010-12-34").build();

        예약_생성_실패(request, 409, "올바른 전화번호 형식이 아닙니다.");
    }

    @DisplayName("예약 생성 - 전화번호에 숫자가 아닌 문자 포함된 경우 실패")
    @Test
    void createReservationFailWithNonNumericPhoneNumber() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withPhoneNumber("010-abcd-5678").build();

        예약_생성_실패(request, 409, "올바른 전화번호 형식이 아닙니다.");
    }

    @DisplayName("예약 생성 - 전화번호 길이가 부족한 경우 실패")
    @Test
    void createReservationFailWithShortPhoneNumber() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().withPhoneNumber("010-1234").build();

        예약_생성_실패(request, 409, "올바른 전화번호 형식이 아닙니다.");
    }

    @DisplayName("예약 생성 - 동일 사이트 동일 날짜 중복 예약 실패")
    @Test
    void createReservationFailWithDuplicateReservation() {
        ReservationRequest firstRequest = new ReservationRequestTestDataBuilder().build();
        예약_생성_성공(firstRequest);
        ReservationRequest duplicateRequest = new ReservationRequestTestDataBuilder()
                .withCustomerName("김철수")
                .withPhoneNumber("010-9876-5432")
                .build();

        예약_생성_실패(duplicateRequest, 409, "해당 기간에 이미 예약이 존재합니다.");
    }

    @DisplayName("예약 취소 - 성공")
    @Test
    void cancelReservation() {
        ReservationRequest request = new ReservationRequestTestDataBuilder().build();
        ReservationResponse reservation = 예약_생성_성공(request);

        var message = 예약_취소_성공(reservation.getId(), reservation.getConfirmationCode());

        assertThat(message).isEqualTo("예약이 취소되었습니다.");
    }

    @DisplayName("예약 생성 - 예약 취소 후 동일 사이트 동일 날짜 예약 성공")
    @Test
    void cancelReservationAndCreateDuplicateReservation() {
        ReservationRequest firstRequest = new ReservationRequestTestDataBuilder().build();
        ReservationResponse response1 = 예약_생성_성공(firstRequest);
        ReservationRequest duplicateRequest = new ReservationRequestTestDataBuilder()
                .withCustomerName("김철수")
                .withPhoneNumber("010-9876-5432")
                .build();
        예약_취소_성공(response1.getId(), response1.getConfirmationCode());

        ReservationResponse response2 = 예약_생성_성공(duplicateRequest);

        assertThat(response2.getCustomerName()).isEqualTo(duplicateRequest.getCustomerName());
        assertThat(response2.getStartDate()).isEqualTo(duplicateRequest.getStartDate());
        assertThat(response2.getEndDate()).isEqualTo(duplicateRequest.getEndDate());
        assertThat(response2.getSiteNumber()).isEqualTo(duplicateRequest.getSiteNumber());
        assertThat(response2.getPhoneNumber()).isEqualTo(duplicateRequest.getPhoneNumber());
    }

    @DisplayName("예약 생성 - 동시성 테스트")
    @Test
    void createReservation_동시성_테스트() {
        // Given
        int threadCount = 10;
        AtomicInteger counter = new AtomicInteger(0);

        // When
        ConcurrencyTestHelper.ConcurrencyTestResult result = ConcurrencyTestHelper.executeConcurrentTasks(
                () -> new ReservationRequestTestDataBuilder().withCustomerName("고객" + counter.incrementAndGet()).build(),
                request -> { 예약_생성_성공(request); return null; },
                threadCount
        );

        // Then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(threadCount - 1);
    }
}
