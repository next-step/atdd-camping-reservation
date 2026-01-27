package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static com.camping.legacy.support.TestDataFactory.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 연박 예약 인수 테스트
 *
 * 인수 조건: "연박 예약 시 시작일부터 종료일까지 모든 날짜가 예약 가능해야만 예약이 성공한다."
 *
 * @see docs/acceptance-criteria.md - 3. 연박 예약 (7점)
 */
@DisplayName("3. 연박 예약 인수 테스트")
class ConsecutiveStayAcceptanceTest extends AcceptanceTest {

    // 테스트용 기간 상수
    private static final int 기본_시작일_오프셋 = 5;
    private static final int 기본_종료일_오프셋 = 7;
    private static final int 최대_예약_기간 = 30;

    // 연박 기간
    private static final int 일박 = 1;
    private static final int 이박 = 2;
    private static final int 삼박 = 3;

    // =========================================================================
    // 검증 포인트
    // =========================================================================

    @Nested
    @DisplayName("검증 포인트")
    class 검증_포인트 {

        @Test
        @Disabled("BUG: 중간 날짜 검증 누락 - 시작일/종료일만 검사함")
        @DisplayName("VP-01: 중간 날짜에 기존 예약 있으면 거부")
        void 중간_날짜에_기존_예약이_있으면_거부된다() {
            // given - 기존 예약: 7~8일
            LocalDate 기존시작일 = daysFromNow(7);
            LocalDate 기존종료일 = daysFromNow(8);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 신규 예약: 5~10일 (기존 예약을 완전히 포함)
            LocalDate 새시작일 = daysFromNow(5);
            LocalDate 새종료일 = daysFromNow(10);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then - 중간에 기존 예약이 있으므로 실패해야 함
            응답_검증_충돌_에러(응답, "해당 기간에 이미 예약이 존재합니다.");
        }

        @Test
        @Disabled("BUG: 중간 날짜 검증 누락")
        @DisplayName("VP-02: 시작일/종료일만 비어있어도 중간 날짜 점유 시 거부")
        void 시작일_종료일이_비어있어도_중간_날짜가_점유되면_거부된다() {
            // given - 기존 예약: 8~9일 (짧은 예약)
            LocalDate 기존시작일 = daysFromNow(8);
            LocalDate 기존종료일 = daysFromNow(9);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 신규 예약: 5~15일 (기존 예약을 완전히 포함하는 긴 기간)
            LocalDate 새시작일 = daysFromNow(5);
            LocalDate 새종료일 = daysFromNow(15);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @Disabled("BUG: 시작일/종료일만 검사, 중간 날짜 미검사")
        @DisplayName("VP-03: 검색 API도 전체 기간 가용 사이트만 반환")
        void 검색_API도_전체_기간_가용_사이트만_반환한다() {
            // given - A1에 중간 날짜 예약 (7~8일)
            LocalDate 기존시작일 = daysFromNow(7);
            LocalDate 기존종료일 = daysFromNow(8);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // 검색 기간: 5~10일 (기존 예약을 완전히 포함)
            LocalDate 검색시작일 = daysFromNow(5);
            LocalDate 검색종료일 = daysFromNow(10);

            // when
            ExtractableResponse<Response> 응답 = 사이트_검색(검색시작일, 검색종료일);

            // then - A1은 중간 날짜에 예약이 있으므로 제외되어야 함
            List<String> 사이트목록 = 응답.jsonPath().getList("siteNumber", String.class);
            assertThat(사이트목록).doesNotContain(SITE_A1);
        }

        @Test
        @DisplayName("VP-04: 3박 예약 시 4일치 모두 검증 - 겹치지 않으면 성공")
        void 삼박_예약시_겹치지_않으면_성공한다() {
            // given - 3박 4일 예약
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_시작일_오프셋 + 삼박);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("startDate")).isEqualTo(시작일.toString());
            assertThat(응답.jsonPath().getString("endDate")).isEqualTo(종료일.toString());
        }
    }

    // =========================================================================
    // 경계값 검증
    // =========================================================================

    @Nested
    @DisplayName("경계값 검증")
    class 경계값_검증 {

        @Test
        @DisplayName("BV-01: 1박(시작일=종료일-1) 허용")
        void 일박_예약은_허용된다() {
            // given - 시작일과 종료일이 연속인 경우 (1박)
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_시작일_오프셋 + 일박);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-02-1: 30박(최대) 허용")
        void 삼십박_예약은_허용된다() {
            // given - 30일 = 최대 허용 기간
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = 시작일.plusDays(최대_예약_기간);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-02-2: 31박 거부")
        void 삼십일박_예약은_거부된다() {
            // given - 31일 = 최대 허용 기간 초과
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = 시작일.plusDays(최대_예약_기간 + 1);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("BV-03: 2박 예약 시 3일치 모두 검증 - 성공 케이스")
        void 이박_예약은_성공한다() {
            // given - 2박 3일 예약
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("startDate")).isEqualTo(시작일.toString());
            assertThat(응답.jsonPath().getString("endDate")).isEqualTo(종료일.toString());
        }
    }

    // =========================================================================
    // 연박 중복 시나리오 테스트
    // =========================================================================

    @Nested
    @DisplayName("연박 중복 시나리오")
    class 연박_중복_시나리오 {

        // 기존 예약: 10~12일 (A1 사이트)
        private static final int 기존_시작일_오프셋 = 10;
        private static final int 기존_종료일_오프셋 = 12;

        @Test
        @DisplayName("기존 예약 이전 기간은 예약 가능하다 (8~9일)")
        void 기존_예약_이전_기간은_예약_가능하다() {
            // given - 기존 예약: 10~12일
            LocalDate 기존시작일 = daysFromNow(기존_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기존_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 8~9일 (겹치지 않음)
            LocalDate 새시작일 = daysFromNow(8);
            LocalDate 새종료일 = daysFromNow(9);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("기존 예약 이후 기간은 예약 가능하다 (13~15일)")
        void 기존_예약_이후_기간은_예약_가능하다() {
            // given - 기존 예약: 10~12일
            LocalDate 기존시작일 = daysFromNow(기존_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기존_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 13~15일 (겹치지 않음)
            LocalDate 새시작일 = daysFromNow(13);
            LocalDate 새종료일 = daysFromNow(15);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("시작 부분이 겹치면 예약할 수 없다 (9~11일)")
        void 시작_부분이_겹치면_예약할_수_없다() {
            // given - 기존 예약: 10~12일
            LocalDate 기존시작일 = daysFromNow(기존_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기존_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 9~11일 (10, 11일 겹침)
            LocalDate 새시작일 = daysFromNow(9);
            LocalDate 새종료일 = daysFromNow(11);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("종료 부분이 겹치면 예약할 수 없다 (11~14일)")
        void 종료_부분이_겹치면_예약할_수_없다() {
            // given - 기존 예약: 10~12일
            LocalDate 기존시작일 = daysFromNow(기존_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기존_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 11~14일 (11, 12일 겹침)
            LocalDate 새시작일 = daysFromNow(11);
            LocalDate 새종료일 = daysFromNow(14);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("완전히 동일한 기간은 예약할 수 없다 (10~12일)")
        void 완전히_동일한_기간은_예약할_수_없다() {
            // given - 기존 예약: 10~12일
            LocalDate 기존시작일 = daysFromNow(기존_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기존_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 10~12일 (완전 동일)
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 기존시작일, 기존종료일, "박영희", "01011112222");

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("기존 예약 직전까지 연박 예약이 가능하다")
        void 기존_예약_직전까지_연박_예약이_가능하다() {
            // given - 기존 예약: 10~12일
            LocalDate 기존시작일 = daysFromNow(기존_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기존_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 신규 예약: 5~9일 (기존 예약 직전까지)
            LocalDate 새시작일 = daysFromNow(5);
            LocalDate 새종료일 = daysFromNow(9);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("기존 예약 직후부터 연박 예약이 가능하다")
        void 기존_예약_직후부터_연박_예약이_가능하다() {
            // given - 기존 예약: 5~7일
            LocalDate 기존시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 신규 예약: 8~12일 (기존 예약 직후부터)
            LocalDate 새시작일 = daysFromNow(8);
            LocalDate 새종료일 = daysFromNow(12);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            응답_검증_성공(응답);
        }
    }

    // =========================================================================
    // 헬퍼 메서드
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

    private ExtractableResponse<Response> 사이트_검색(LocalDate startDate, LocalDate endDate) {
        return given()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when()
                .get("/api/sites/search")
                .then()
                .extract();
    }

    // =========================================================================
    // Assertion 헬퍼
    // =========================================================================

    private void 응답_검증_성공(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    private void 응답_검증_충돌_에러(ExtractableResponse<Response> response, String expectedMessage) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo(expectedMessage);
    }
}
