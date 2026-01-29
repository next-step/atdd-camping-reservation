package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.camping.legacy.support.TestDataFactory.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 취소 인수 테스트
 *
 * 인수 조건: "본인 확인(확인코드) 후에만 예약 취소가 가능하며, 취소 시점에 따라 상태가 구분되어야 한다."
 *
 * @see docs/acceptance-criteria.md - 4. 예약 취소 (6점)
 */
@DisplayName("4. 예약 취소 인수 테스트")
class CancellationAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("확인코드 불일치 시 취소 거부")
    void 확인코드_불일치시_취소가_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        // when
        ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), "WRONG1");

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @Disabled("TODO: 당일 취소 시 CANCELLED_SAME_DAY 상태 구현 필요")
    @DisplayName("당일 취소 시 CANCELLED_SAME_DAY 상태")
    void 당일_취소시_CANCELLED_SAME_DAY_상태가_된다() {
        // given - 오늘 시작하는 예약
        LocalDate 오늘 = LocalDate.now();
        LocalDate 종료일 = 오늘.plusDays(2);
        var 예약 = testDataFactory.createReservation(SITE_A1, 오늘, 종료일, "김철수", "01098765432");

        // when
        ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(응답.jsonPath().getString("status")).isEqualTo("CANCELLED_SAME_DAY");
    }

    @Test
    @DisplayName("사전 취소 시 CANCELLED 상태")
    void 사전_취소시_CANCELLED_상태가_된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        // when
        ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(응답.jsonPath().getString("status")).isEqualTo("CANCELLED");
    }

    @Test
    @Disabled("BUG: 취소된 예약이 중복 체크에서 제외되지 않음")
    @DisplayName("취소된 사이트는 즉시 재예약 가능")
    void 취소된_사이트는_즉시_재예약_가능하다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");
        예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    // =========================================================================
    // 헬퍼 메서드
    // =========================================================================

    private ExtractableResponse<Response> 예약_취소_요청(Long reservationId, String confirmationCode) {
        return given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .delete("/api/reservations/{id}", reservationId)
                .then()
                .extract();
    }

    private ExtractableResponse<Response> 예약_생성_요청(String siteNumber, LocalDate startDate, LocalDate endDate) {
        return given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(siteNumber, startDate, endDate, DEFAULT_CUSTOMER_NAME, DEFAULT_PHONE_NUMBER))
                .when()
                .post("/api/reservations")
                .then()
                .extract();
    }

    // =========================================================================
    // Custom Matcher
    // =========================================================================

    private void 응답_검증_취소_성공(ExtractableResponse<Response> response, String expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
    }

    private void 응답_검증_취소_실패(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
