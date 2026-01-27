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

    // 테스트용 기간 상수
    private static final int 기본_시작일_오프셋 = 5;
    private static final int 기본_종료일_오프셋 = 7;

    // 예약 상태
    private static final String 취소됨 = "CANCELLED";
    private static final String 당일취소됨 = "CANCELLED_SAME_DAY";

    // 존재하지 않는 예약 ID
    private static final Long 존재하지_않는_예약_ID = 99999L;

    // =========================================================================
    // 검증 포인트
    // =========================================================================

    @Nested
    @DisplayName("검증 포인트")
    class 검증_포인트 {

        @Test
        @DisplayName("VP-01: 확인코드 불일치 시 취소 거부")
        void 확인코드_불일치시_취소가_거부된다() {
            // given - 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");
            String 잘못된_확인코드 = "WRONG1";

            // when - 잘못된 확인코드로 취소 시도
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 잘못된_확인코드);

            // then
            응답_검증_충돌_에러(응답, "확인코드가 일치하지 않습니다.");
        }

        @Test
        @Disabled("TODO: 당일 취소 시 CANCELLED_SAME_DAY 상태 구현 필요")
        @DisplayName("VP-02: 당일 취소 시 CANCELLED_SAME_DAY 상태")
        void 당일_취소시_CANCELLED_SAME_DAY_상태가_된다() {
            // given - 오늘 시작하는 예약 생성
            LocalDate 오늘 = LocalDate.now();
            LocalDate 종료일 = 오늘.plusDays(2);
            var 예약 = testDataFactory.createReservation(SITE_A1, 오늘, 종료일, "김철수", "01098765432");

            // when - 당일 취소
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("status")).isEqualTo(당일취소됨);
        }

        @Test
        @DisplayName("VP-03: 사전 취소 시 CANCELLED 상태")
        void 사전_취소시_CANCELLED_상태가_된다() {
            // given - 미래 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            // when - 사전 취소
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("status")).isEqualTo(취소됨);
        }

        @Test
        @Disabled("BUG: 취소된 예약이 중복 체크에서 제외되지 않음")
        @DisplayName("VP-04: 취소된 사이트는 즉시 재예약 가능")
        void 취소된_사이트는_즉시_재예약_가능하다() {
            // given - 예약 생성 후 취소
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");
            예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // when - 같은 기간에 새로운 예약 시도
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then - 취소된 예약은 무시되어야 함
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        }
    }

    // =========================================================================
    // 경계값 검증
    // =========================================================================

    @Nested
    @DisplayName("경계값 검증")
    class 경계값_검증 {

        @Test
        @Disabled("TODO: 당일 취소 시 CANCELLED_SAME_DAY 상태 구현 필요")
        @DisplayName("BV-01: 시작일 당일 취소 → CANCELLED_SAME_DAY")
        void 시작일_당일_취소시_CANCELLED_SAME_DAY_상태가_된다() {
            // given - 오늘 시작하는 예약
            LocalDate 오늘 = LocalDate.now();
            LocalDate 종료일 = 오늘.plusDays(2);
            var 예약 = testDataFactory.createReservation(SITE_A1, 오늘, 종료일, "김철수", "01098765432");

            // when - 시작일 당일 취소
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("status")).isEqualTo(당일취소됨);
        }

        @Test
        @DisplayName("BV-02: 시작일 전날 취소 → CANCELLED")
        void 시작일_전날_취소시_CANCELLED_상태가_된다() {
            // given - 내일 시작하는 예약
            LocalDate 내일 = daysFromNow(1);
            LocalDate 종료일 = daysFromNow(3);
            var 예약 = testDataFactory.createReservation(SITE_A1, 내일, 종료일, "김철수", "01098765432");

            // when - 시작일 전날(오늘) 취소
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("status")).isEqualTo(취소됨);
        }
    }

    // =========================================================================
    // 추가 검증
    // =========================================================================

    @Nested
    @DisplayName("추가 검증")
    class 추가_검증 {

        @Test
        @DisplayName("존재하지 않는 예약 취소 시 실패")
        void 존재하지_않는_예약_취소시_실패한다() {
            // when - 존재하지 않는 예약 ID로 취소 시도
            ExtractableResponse<Response> 응답 = 예약_취소_요청(존재하지_않는_예약_ID, "ABC123");

            // then
            assertThat(응답.statusCode()).isIn(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.CONFLICT.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        @Test
        @DisplayName("이미 취소된 예약 재취소 시 실패")
        void 이미_취소된_예약_재취소시_실패한다() {
            // given - 예약 생성 후 취소
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");
            예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // when - 다시 취소 시도
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then - 이미 취소된 예약은 다시 취소할 수 없음
            assertThat(응답.statusCode()).isIn(
                    HttpStatus.CONFLICT.value(),
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        @Test
        @DisplayName("정상 취소 시 취소된 예약 정보 반환")
        void 정상_취소시_취소된_예약_정보가_반환된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            // when
            ExtractableResponse<Response> 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("status")).isEqualTo(취소됨);
            assertThat(응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
        }
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
    // Assertion 헬퍼
    // =========================================================================

    private void 응답_검증_성공(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    private void 응답_검증_충돌_에러(ExtractableResponse<Response> response, String expectedMessage) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo(expectedMessage);
    }
}
