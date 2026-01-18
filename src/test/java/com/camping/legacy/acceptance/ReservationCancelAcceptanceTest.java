package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.camping.legacy.acceptance.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;
import static com.camping.legacy.acceptance.TestFixture.*;

public class ReservationCancelAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("올바른 확인코드로 예약 취소 성공")
    void 예약_취소_성공() {
        // Given - 예약 생성
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 취소 성공 검증
        cancelReservation(reservationId, confirmationCode);

        // Then - 취소 후 상태 확인
        var getResponse = getReservation(reservationId);
        var status = getResponse.jsonPath().getString("status");
        assertThat(status).isIn("CANCELLED", "CANCELLED_SAME_DAY");
    }

    @Test
    @DisplayName("체크인 당일 취소 시 CANCELLED_SAME_DAY 상태")
    void 당일_취소_상태() {
        // Given - 오늘 시작하는 예약
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 0, 2);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 당일 취소
        cancelReservation(reservationId, confirmationCode);

        // Then - 당일 취소 상태
        var getResponse = getReservation(reservationId);
        var status = getResponse.jsonPath().getString("status");
        assertThat(status).isEqualTo("CANCELLED_SAME_DAY");
    }

    @Test
    @DisplayName("체크인 전날 취소 시 CANCELLED 상태")
    void 사전_취소_상태() {
        // Given - 내일 시작하는 예약
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 1, 3);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 사전 취소
        cancelReservation(reservationId, confirmationCode);

        // Then - 일반 취소 상태
        var getResponse = getReservation(reservationId);
        var status = getResponse.jsonPath().getString("status");
        assertThat(status).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("잘못된 확인코드로 취소 시 실패")
    void 잘못된_확인코드_취소_실패() {
        // Given - 예약 생성
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");

        // When - 잘못된 확인코드로 취소 요청
        var cancelResponse = cancelReservation(reservationId, "WRONG1");

        // Then - 실패 응답 검증
        assertThat(cancelResponse.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("존재하지 않는 예약 취소 시 실패")
    void 존재하지_않는_예약_취소_실패() {
        // When - 존재하지 않는 예약 취소 요청
        var cancelResponse = cancelReservation(99999L, "ABC123");

        // Then - 실패 응답 검증
        assertThat(cancelResponse.statusCode()).isEqualTo(400);
    }

    @ParameterizedTest(name = "예약 시작 {0}일 후 취소 → {1}")
    @CsvSource({
            "0, CANCELLED_SAME_DAY",
            "1, CANCELLED",
            "7, CANCELLED",
    })
    @DisplayName("취소 시점에 따른 상태 테스트")
    void 취소_상태_테스트(int startDay, String expectedStatus) {
        // Given
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, startDay, startDay + 2);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When
        cancelReservation(reservationId, confirmationCode);

        // Then
        var getResponse = getReservation(reservationId);
        assertThat(getResponse.jsonPath().getString("status")).isEqualTo(expectedStatus);
    }

}
