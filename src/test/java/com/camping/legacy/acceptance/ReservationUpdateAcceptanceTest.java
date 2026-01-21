package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.fixture.TestFixture.*;
import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_수정;
import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_요청;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 수정 인수 테스트
 *
 * 인수 조건:
 * "예약자는 확인 코드를 통해 예약 정보를 수정할 수 있어야 한다"
 */
@DisplayName("예약 수정 인수 테스트")
class ReservationUpdateAcceptanceTest extends AcceptanceTest {

    private static final LocalDate BASE_DATE = LocalDate.now().plusDays(30);

    @Test
    @DisplayName("예약 날짜를 수정한다")
    void 예약_수정_성공() {
        // Given - 예약 생성
        LocalDate startDate = BASE_DATE;
        LocalDate endDate = BASE_DATE.plusDays(1);
        String siteNumber = 사이트_A1;

        // 1. 홍길동이 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, 홍길동, 홍길동_전화번호, startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 날짜 변경
        LocalDate newStartDate = BASE_DATE.plusDays(5);
        LocalDate newEndDate = BASE_DATE.plusDays(6);

        ExtractableResponse<Response> response = 예약_수정(
                reservationId, confirmationCode,
                siteNumber, 홍길동, 홍길동_전화번호, newStartDate, newEndDate);

        // Then
        assertThat(response.statusCode())
                .as("예약 날짜 수정이 성공해야 한다")
                .isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("다른 사이트로 변경한다")
    void 다른_사이트로_변경_성공() {
        // Given - 예약 생성
        LocalDate startDate = BASE_DATE.plusDays(10);
        LocalDate endDate = BASE_DATE.plusDays(11);
        String siteNumber = 사이트_A1;
        String newSiteNumber = 사이트_A2;

        // 1. 홍길동이 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, 홍길동, 홍길동_전화번호, startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 사이트 변경
        ExtractableResponse<Response> response = 예약_수정(
                reservationId, confirmationCode,
                newSiteNumber, 홍길동, 홍길동_전화번호, startDate, endDate);

        // Then
        assertThat(response.statusCode())
                .as("다른 사이트로 변경이 성공해야 한다")
                .isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber"))
                .isEqualTo(newSiteNumber);
    }

    @Test
    @DisplayName("잘못된 확인 코드로 수정 시도시 실패한다")
    void 잘못된_확인코드_수정_실패() {
        // Given - 예약 생성
        LocalDate startDate = BASE_DATE.plusDays(20);
        LocalDate endDate = BASE_DATE.plusDays(21);
        String siteNumber = 사이트_A1;

        // 1. 홍길동이 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, 홍길동, 홍길동_전화번호, startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");
        String wrongConfirmationCode = "WRONG1";

        // When - 잘못된 확인 코드로 수정 시도
        ExtractableResponse<Response> response = 예약_수정(
                reservationId, wrongConfirmationCode,
                siteNumber, 홍길동, 홍길동_전화번호, startDate.plusDays(1), endDate.plusDays(1));

        // Then
        assertThat(response.statusCode())
                .as("잘못된 확인 코드로 수정 시도시 실패해야 한다")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("이미 예약된 기간으로 변경 시도시 실패한다")
    void 기간_겹침_수정_실패() {
        // Given - 두 개의 예약 생성
        LocalDate firstStart = BASE_DATE.plusDays(30);
        LocalDate firstEnd = BASE_DATE.plusDays(31);
        LocalDate secondStart = BASE_DATE.plusDays(35);
        LocalDate secondEnd = BASE_DATE.plusDays(36);
        String siteNumber = 사이트_A1;

        // 1. 홍길동이 첫 번째 예약 생성
        ExtractableResponse<Response> firstResponse = 예약_요청(
                siteNumber, 홍길동, 홍길동_전화번호, firstStart, firstEnd);
        assertThat(firstResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // 2. 김철수가 두 번째 예약 생성
        ExtractableResponse<Response> secondResponse = 예약_요청(
                siteNumber, 김철수, 김철수_전화번호, secondStart, secondEnd);
        assertThat(secondResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long secondReservationId = secondResponse.jsonPath().getLong("id");
        String secondConfirmationCode = secondResponse.jsonPath().getString("confirmationCode");

        // When - 두 번째 예약을 첫 번째 예약 기간으로 변경 시도
        ExtractableResponse<Response> response = 예약_수정(
                secondReservationId, secondConfirmationCode,
                siteNumber, 김철수, 김철수_전화번호, firstStart, firstEnd);

        // Then
        assertThat(response.statusCode())
                .as("이미 예약된 기간으로 변경 시도시 실패해야 한다")
                .isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
    }
}
