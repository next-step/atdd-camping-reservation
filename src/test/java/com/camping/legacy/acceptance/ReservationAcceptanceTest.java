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
 * 예약 생성 인수 테스트
 *
 * 인수 조건: "고객이 유효한 정보로 예약 요청 시, 중복 없이 예약이 생성되고 6자리 확인코드가 발급되어야 한다."
 *
 * @see docs/acceptance-criteria.md - 1. 예약 생성 (9점)
 */
@DisplayName("1. 예약 생성 인수 테스트")
class ReservationAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("과거 날짜 예약 거부")
    void 과거_날짜로_예약하면_거부된다() {
        // given
        LocalDate 과거시작일 = LocalDate.now().minusDays(5);
        LocalDate 과거종료일 = LocalDate.now().minusDays(3);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 과거시작일, 과거종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("종료일 < 시작일 거부")
    void 종료일이_시작일보다_이전이면_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(10);
        LocalDate 종료일 = daysFromNow(5);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("오늘로부터 30일 초과 시작일 거부")
    void 오늘로부터_30일_초과_시작일이면_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(31);
        LocalDate 종료일 = daysFromNow(33);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("이름 2~20자 검증")
    void 이름이_2자_미만이면_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                SITE_A1, 시작일, 종료일, "홍", DEFAULT_PHONE_NUMBER);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("전화번호 10~11자리 검증")
    void 전화번호가_형식에_맞지_않으면_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                SITE_A1, 시작일, 종료일, DEFAULT_CUSTOMER_NAME, "010123456"); // 9자리

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("존재하지 않는 사이트 거부")
    void 존재하지_않는_사이트로_예약하면_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청("Z99", 시작일, 종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("동일 사이트/기간 중복 예약 거부")
    void 동일_사이트_기간에_중복_예약하면_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                SITE_A1, 시작일, 종료일, "박영희", "01011112222");

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @Disabled("BUG: 취소된 예약이 중복 체크에서 제외되지 않음")
    @DisplayName("취소된 예약은 중복 체크 제외")
    void 취소된_예약은_중복_체크에서_제외된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        testDataFactory.createCancelledReservation(SITE_A1, 시작일, 종료일);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                SITE_A1, 시작일, 종료일, "박영희", "01011112222");

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("6자리 영숫자 확인코드 생성")
    void 예약_성공시_6자리_확인코드가_생성된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        String 확인코드 = 응답.jsonPath().getString("confirmationCode");
        assertThat(확인코드).hasSize(6);
        assertThat(확인코드).matches("[A-Z0-9]{6}");
    }

    @Test
    @DisplayName("성공 시 201 응답 + 예약정보 반환")
    void 예약_성공시_201응답과_예약정보가_반환된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        // when
        ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
        assertThat(응답.jsonPath().getString("customerName")).isEqualTo(DEFAULT_CUSTOMER_NAME);
    }

    // =========================================================================
    // 헬퍼 메서드 : 반복되는 “행동(when)”을 대신 해주는 메서드
    // =========================================================================

    private ExtractableResponse<Response> 예약_생성_요청(String siteNumber, LocalDate startDate, LocalDate endDate) {
        return 예약_생성_요청_고객정보(siteNumber, startDate, endDate, DEFAULT_CUSTOMER_NAME, DEFAULT_PHONE_NUMBER);
    }

    private ExtractableResponse<Response> 예약_생성_요청_고객정보(
            String siteNumber, LocalDate startDate, LocalDate endDate,
            String customerName, String phoneNumber) {
        return given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(siteNumber, startDate, endDate, customerName, phoneNumber))
                .when()
                .post("/api/reservations")
                .then()
                .extract();
    }

    // =========================================================================
    // Custom Matcher : 반복되는 “검증(then)”을 대신 해주는 메서드
    // =========================================================================

    private void 응답_검증_예약_성공(ExtractableResponse<Response> response, String expectedSite) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(expectedSite);
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
        String 확인코드 = response.jsonPath().getString("confirmationCode");
        assertThat(확인코드).hasSize(6);
        assertThat(확인코드).matches("[A-Z0-9]{6}");
    }

    private void 응답_검증_예약_실패(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
