package com.camping.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.camping.acceptance.steps.ReservationSteps.예약_요청;
import static com.camping.acceptance.steps.ReservationSteps.예약_취소;
import static com.camping.acceptance.steps.SiteSteps.가용_사이트_조회;
import static org.assertj.core.api.Assertions.assertThat;
import com.camping.legacy.CampingApplication;

/**
 * 취소 후 재예약 인수 테스트
 *
 * 인수 조건 P0-3:
 * "취소된 예약이 있는 기간은 새 예약이 가능해야 한다"
 */
@DisplayName("취소 후 재예약 인수 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = CampingApplication.class)
class CancelRebookAcceptanceTest {

    @LocalServerPort
    private int port;

    private static final LocalDate BASE_DATE = LocalDate.now().plusDays(70);

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("취소된 예약이 있는 날짜에 새로운 예약이 가능하다")
    void 취소된_예약이_있는_날짜에_새로운_예약이_가능하다() {
        // Given - 예약 생성 후 취소
        LocalDate startDate = BASE_DATE;
        LocalDate endDate = BASE_DATE.plusDays(1);
        String siteNumber = "A-3";

        // 1. 홍길동이 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // 2. 홍길동이 예약 취소
        ExtractableResponse<Response> cancelResponse = 예약_취소(reservationId, confirmationCode);
        assertThat(cancelResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // When - 최지원이 같은 기간에 새로운 예약 시도
        ExtractableResponse<Response> newReservation = 예약_요청(
                siteNumber, "최지원", "010-9999-8888", startDate, endDate);

        // Then - 새 예약이 성공해야 함
        assertThat(newReservation.statusCode())
                .as("취소된 예약이 있는 날짜에 새로운 예약이 가능해야 한다")
                .isEqualTo(HttpStatus.CREATED.value());

        // 새 예약자는 새로운 6자리 확인 코드를 받아야 함
        String newConfirmationCode = newReservation.jsonPath().getString("confirmationCode");
        assertThat(newConfirmationCode)
                .as("새 예약자는 새로운 6자리 확인 코드를 받는다")
                .hasSize(6)
                .isNotEqualTo(confirmationCode);
    }

    @Test
    @DisplayName("당일 취소된 예약이 있는 날짜에 새로운 예약이 가능하다")
    void 당일_취소된_예약이_있는_날짜에_새로운_예약이_가능하다() {
        // Given - 오늘 날짜로 예약 생성 후 당일 취소
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(1);
        String siteNumber = "A-3";

        // 1. 홍길동이 오늘~내일 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // 2. 홍길동이 당일 예약 취소 (CANCELLED_SAME_DAY 상태가 됨)
        ExtractableResponse<Response> cancelResponse = 예약_취소(reservationId, confirmationCode);
        assertThat(cancelResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // When - 최지원이 같은 기간에 새로운 예약 시도
        ExtractableResponse<Response> newReservation = 예약_요청(
                siteNumber, "최지원", "010-9999-8888", startDate, endDate);

        // Then - 새 예약이 성공해야 함
        assertThat(newReservation.statusCode())
                .as("당일 취소된 예약이 있는 날짜에 새로운 예약이 가능해야 한다")
                .isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("활성 예약이 있는 날짜에는 새 예약이 실패한다")
    void 활성_예약이_있는_날짜에는_새_예약이_실패한다() {
        // Given - CONFIRMED 상태 예약 존재
        LocalDate startDate = BASE_DATE.plusDays(10);
        LocalDate endDate = BASE_DATE.plusDays(11);
        String siteNumber = "A-3";

        ExtractableResponse<Response> existingReservation = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", startDate, endDate);
        assertThat(existingReservation.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // When - 같은 기간에 새로운 예약 시도
        ExtractableResponse<Response> newReservation = 예약_요청(
                siteNumber, "최지원", "010-9999-8888", startDate, endDate);

        // Then - 예약 실패해야 함
        assertThat(newReservation.statusCode())
                .as("활성 예약이 있는 날짜에는 새 예약이 실패해야 한다")
                .isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("취소 후 해당 사이트가 가용 사이트 검색에 포함된다")
    void 취소_후_해당_사이트가_가용_사이트_검색에_포함된다() {
        // Given - 예약 생성 후 취소
        LocalDate startDate = BASE_DATE.plusDays(20);
        LocalDate endDate = BASE_DATE.plusDays(21);
        String siteNumber = "A-3";

        // 1. 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // 2. 예약 취소
        ExtractableResponse<Response> cancelResponse = 예약_취소(reservationId, confirmationCode);
        assertThat(cancelResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // When - 가용 사이트 검색
        List<Map<String, Object>> availableSites = 가용_사이트_조회(startDate);

        // Then - 취소된 사이트가 검색 결과에 포함되어야 함
        boolean containsSite = availableSites.stream()
                .anyMatch(site -> siteNumber.equals(site.get("siteNumber")));

        assertThat(containsSite)
                .as("취소 후 해당 사이트(%s)가 가용 사이트 검색에 포함되어야 한다", siteNumber)
                .isTrue();
    }
}
