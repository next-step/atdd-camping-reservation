package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.HashMap;

import static com.camping.legacy.acceptance.ReservationSteps.createReservation;
import static com.camping.legacy.acceptance.ReservationSteps.updateReservation;
import static com.camping.legacy.acceptance.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationUpdateAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("올바른 확인코드로 예약 날짜 수정 성공")
    void 예약_수정_성공() {
        // Given - 예약 생성
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 날짜 변경 요청
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("startDate", LocalDate.now().plusDays(10).toString());
        updateRequest.put("endDate", LocalDate.now().plusDays(12).toString());

        var updateResponse = updateReservation(reservationId, confirmationCode, updateRequest);

        // Then - 수정 성공 검증
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.jsonPath().getString("startDate"))
                .isEqualTo(LocalDate.now().plusDays(10).toString());
    }

    @Test
    @DisplayName("다른 사이트로 변경 성공")
    void 다른_사이트로_변경_성공() {
        // Given - A-1 예약
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 10, 15);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - B-1으로 변경
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("siteNumber", SITE_B1);

        var updateResponse = updateReservation(reservationId, confirmationCode, updateRequest);

        // Then - 수정 성공 검증
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.jsonPath().getString("siteNumber"))
                .isEqualTo(SITE_B1);
    }

    @Test
    @DisplayName("잘못된 확인코드로 수정 시 실패")
    void 잘못된_확인코드_수정_실패() {
        // Given - 예약 생성
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");

        // When - 잘못된 확인코드로 수정 요청
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("startDate", LocalDate.now().plusDays(10).toString());

        var updateResponse = updateReservation(reservationId, INVALID_CONFIRMATION_CODE, updateRequest);

        // Then - 실패 응답 검증
        assertThat(updateResponse.statusCode()).isEqualTo(400);
    }

    @ParameterizedTest(name = "{0}: {1}~{2}일 → {3}")
    @CsvSource({
            "앞쪽 겹침, 8, 12, 400",
            "뒤쪽 겹침, 12, 17, 400",
            "내부 포함, 11, 14, 400",
            "외부 포함, 8, 18, 400",
            "앞쪽 인접, 5, 9, 200",
            "뒤쪽 인접, 16, 20, 200"
    })
    @DisplayName("기간 겹침 엣지케이스")
    void 기간_겹침_엣지케이스(String caseName, int startDay, int endDay, int expectedCode) {
        // Given - 기존 예약 (A-1, 10~15일)
        createReservation(OTHER_CUSTOMER, SITE_A1, 10, 15);
        var response = createReservation(DEFAULT_CUSTOMER, SITE_A1, 20, 25);
        var reservationId = response.jsonPath().getLong("id");
        var confirmationCode = response.jsonPath().getString("confirmationCode");

        // When
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("startDate", LocalDate.now().plusDays(startDay).toString());
        updateRequest.put("endDate", LocalDate.now().plusDays(endDay).toString());

        var updateResponse = updateReservation(reservationId, confirmationCode, updateRequest);

        // Then
        assertThat(updateResponse.statusCode()).isEqualTo(expectedCode);
    }
}
