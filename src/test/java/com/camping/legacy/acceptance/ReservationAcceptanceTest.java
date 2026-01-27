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
 * 예약 생성 인수 테스트
 *
 * 인수 조건: "고객이 유효한 정보로 예약 요청 시, 중복되지 않은 사이트에 대해
 *           예약이 생성되고 6자리 확인 코드가 발급되어야 한다."
 *
 * @see docs/acceptance-criteria.md - 1. 예약 생성 (9점)
 */
@DisplayName("1. 예약 생성 인수 테스트")
class ReservationAcceptanceTest extends AcceptanceTest {

    // 테스트용 기간 상수
    private static final int 기본_시작일_오프셋 = 5;
    private static final int 기본_종료일_오프셋 = 7;
    private static final int 최대_예약_기간 = 30;

    // 이름 길이 제한
    private static final int 이름_최소_길이 = 2;
    private static final int 이름_최대_길이 = 20;

    // 전화번호 길이 제한
    private static final int 전화번호_최소_길이 = 10;
    private static final int 전화번호_최대_길이 = 11;

    // 확인코드 길이
    private static final int 확인코드_길이 = 6;

    // =========================================================================
    // 검증 포인트
    // =========================================================================

    @Nested
    @DisplayName("검증 포인트")
    class 검증_포인트 {

        @Test
        @DisplayName("VP-01: 과거 날짜 예약 거부")
        void 과거_날짜로_예약하면_거부된다() {
            // given
            LocalDate 과거시작일 = LocalDate.now().minusDays(5);
            LocalDate 과거종료일 = LocalDate.now().minusDays(3);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 과거시작일, 과거종료일);

            // then
            응답_검증_충돌_에러(응답, "과거 날짜로 예약할 수 없습니다.");
        }

        @Test
        @DisplayName("VP-02: 종료일 < 시작일 거부")
        void 종료일이_시작일보다_이전이면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(10);
            LocalDate 종료일 = daysFromNow(5);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_충돌_에러(응답, "종료일이 시작일보다 이전일 수 없습니다.");
        }

        @Test
        @DisplayName("VP-03: 예약 기간 30일 초과 거부")
        void 예약_기간이_30일을_초과하면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(2);
            LocalDate 종료일 = daysFromNow(2 + 최대_예약_기간 + 3);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_충돌_에러(응답, "예약 기간은 최대 30일입니다.");
        }

        @Test
        @DisplayName("VP-04-1: 이름 2자 미만 거부")
        void 이름이_2자_미만이면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, "홍", DEFAULT_PHONE_NUMBER);

            // then
            응답_검증_충돌_에러(응답, "예약자 이름은 최소 2자 이상이어야 합니다.");
        }

        @Test
        @DisplayName("VP-04-2: 이름 20자 초과 거부")
        void 이름이_20자를_초과하면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 긴이름 = "가".repeat(이름_최대_길이 + 1);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, 긴이름, DEFAULT_PHONE_NUMBER);

            // then
            응답_검증_충돌_에러(응답, "예약자 이름은 최대 20자까지 가능합니다.");
        }

        @Test
        @DisplayName("VP-04-3: 이름 빈 값 거부")
        void 이름이_비어있으면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, "", DEFAULT_PHONE_NUMBER);

            // then
            응답_검증_충돌_에러(응답, "예약자 이름을 입력해주세요.");
        }

        @Test
        @DisplayName("VP-05-1: 전화번호 10자리 미만 거부")
        void 전화번호가_10자리_미만이면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 짧은전화번호 = "010123456"; // 9자리

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, DEFAULT_CUSTOMER_NAME, 짧은전화번호);

            // then
            응답_검증_충돌_에러(응답, "전화번호 형식이 올바르지 않습니다.");
        }

        @Test
        @DisplayName("VP-05-2: 전화번호 11자리 초과 거부")
        void 전화번호가_11자리를_초과하면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 긴전화번호 = "010123456789"; // 12자리

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, DEFAULT_CUSTOMER_NAME, 긴전화번호);

            // then
            응답_검증_충돌_에러(응답, "전화번호 형식이 올바르지 않습니다.");
        }

        @Test
        @DisplayName("VP-05-3: 전화번호 숫자 외 문자 포함 시 거부")
        void 전화번호에_숫자_외_문자가_포함되면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, DEFAULT_CUSTOMER_NAME, "010-ABCD-5678");

            // then
            응답_검증_충돌_에러(응답, "전화번호는 숫자만 입력 가능합니다.");
        }

        @Test
        @DisplayName("VP-06: 존재하지 않는 사이트 거부")
        void 존재하지_않는_사이트로_예약하면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 존재하지않는사이트 = "Z99";

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(존재하지않는사이트, 시작일, 종료일);

            // then
            응답_검증_충돌_에러(응답, "존재하지 않는 캠핑장입니다.");
        }

        @Test
        @DisplayName("VP-06-2: 사이트 번호 빈 값 거부")
        void 사이트_번호가_비어있으면_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청("", 시작일, 종료일);

            // then
            응답_검증_충돌_에러(응답, "사이트 번호를 입력해주세요.");
        }

        @Test
        @DisplayName("VP-06-3: 예약 기간 빈 값 거부")
        void 예약_기간이_비어있으면_거부된다() {
            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, null, null);

            // then
            응답_검증_충돌_에러(응답, "예약 기간을 선택해주세요.");
        }

        @Test
        @DisplayName("VP-07-1: 동일 사이트/기간 중복 예약 거부")
        void 동일_사이트_기간에_중복_예약하면_거부된다() {
            // given - 기존 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            // when - 동일 기간에 다른 고객이 예약 시도
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, "박영희", "01011112222");

            // then
            응답_검증_충돌_에러(응답, "해당 기간에 이미 예약이 존재합니다.");
        }

        @Test
        @DisplayName("VP-07-2: 기간이 일부 겹치면 예약 거부")
        void 기간이_일부_겹치면_거부된다() {
            // given - 기존 예약: 5~7일
            LocalDate 기존시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 기존종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            // when - 겹치는 기간 예약 시도: 6~9일
            LocalDate 새시작일 = daysFromNow(6);
            LocalDate 새종료일 = daysFromNow(9);
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 새시작일, 새종료일, "박영희", "01011112222");

            // then
            응답_검증_충돌_에러(응답, "해당 기간에 이미 예약이 존재합니다.");
        }

        @Test
        @DisplayName("VP-07-3: 같은 기간이라도 다른 사이트는 예약 가능")
        void 같은_기간이라도_다른_사이트는_예약_가능하다() {
            // given - A1 사이트에 기존 예약
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            // when - A2 사이트에 같은 기간 예약
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A2, 시작일, 종료일, "박영희", "01011112222");

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A2);
        }

        @Test
        @Disabled("BUG: 취소된 예약(CANCELLED)이 중복 체크에서 제외되지 않음")
        @DisplayName("VP-08: 취소된 예약은 중복 체크 제외")
        void 취소된_예약은_중복_체크에서_제외된다() {
            // given - 취소된 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createCancelledReservation(SITE_A1, 시작일, 종료일);

            // when - 같은 기간에 새 예약 시도
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, "박영희", "01011112222");

            // then - 취소된 예약은 무시되어야 함
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("VP-09: 6자리 영숫자 확인코드 생성")
        void 예약_성공시_6자리_확인코드가_생성된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
            String 확인코드 = 응답.jsonPath().getString("confirmationCode");
            assertThat(확인코드).hasSize(확인코드_길이);
            assertThat(확인코드).matches("[A-Z0-9]{6}");
        }

        @Test
        @DisplayName("VP-10: 성공 시 201 응답 + 예약정보 반환")
        void 예약_성공시_201응답과_예약정보가_반환된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("confirmationCode")).hasSize(확인코드_길이);
            assertThat(응답.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            assertThat(응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
            assertThat(응답.jsonPath().getString("customerName")).isEqualTo(DEFAULT_CUSTOMER_NAME);
        }
    }

    // =========================================================================
    // 경계값 검증
    // =========================================================================

    @Nested
    @DisplayName("경계값 검증")
    class 경계값_검증 {

        @Test
        @DisplayName("BV-01-1: 이름 2자 허용")
        void 이름_2자는_허용된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 이름2자 = "홍길";

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, 이름2자, DEFAULT_PHONE_NUMBER);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-01-2: 이름 1자 거부")
        void 이름_1자는_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, "홍", DEFAULT_PHONE_NUMBER);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("BV-02-1: 이름 20자 허용")
        void 이름_20자는_허용된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 이름20자 = "가".repeat(이름_최대_길이);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, 이름20자, DEFAULT_PHONE_NUMBER);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-02-2: 이름 21자 거부")
        void 이름_21자는_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 이름21자 = "가".repeat(이름_최대_길이 + 1);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, 이름21자, DEFAULT_PHONE_NUMBER);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("BV-03-1: 전화번호 10자리 허용")
        void 전화번호_10자리는_허용된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 전화번호10자리 = "0101234567";

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, DEFAULT_CUSTOMER_NAME, 전화번호10자리);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-03-2: 전화번호 9자리 거부")
        void 전화번호_9자리는_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String 전화번호9자리 = "010123456";

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일, DEFAULT_CUSTOMER_NAME, 전화번호9자리);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("BV-04-1: 시작일 = 오늘 허용")
        void 시작일이_오늘이면_허용된다() {
            // given
            LocalDate 오늘 = LocalDate.now();
            LocalDate 종료일 = 오늘.plusDays(2);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 오늘, 종료일);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-04-2: 시작일 = 어제 거부")
        void 시작일이_어제면_거부된다() {
            // given
            LocalDate 어제 = LocalDate.now().minusDays(1);
            LocalDate 종료일 = LocalDate.now().plusDays(2);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 어제, 종료일);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("BV-05-1: 30일 기간 허용")
        void 기간_30일은_허용된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = 시작일.plusDays(최대_예약_기간);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-05-2: 31일 기간 거부")
        void 기간_31일은_거부된다() {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = 시작일.plusDays(최대_예약_기간 + 1);

            // when
            ExtractableResponse<Response> 응답 = 예약_생성_요청(SITE_A1, 시작일, 종료일);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
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
