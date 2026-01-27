package com.camping.legacy.acceptance;

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
 * 가용성 확인 인수 테스트
 *
 * 인수 조건: "조회 시점에 실제 예약 가능한 사이트만 정확하게 반환되어야 한다."
 *
 * @see docs/acceptance-criteria.md - 2. 가용성 확인 (8점)
 */
@DisplayName("2. 가용성 확인 인수 테스트")
class AvailabilityAcceptanceTest extends AcceptanceTest {

    // 테스트용 기간 상수
    private static final int 기본_시작일_오프셋 = 5;
    private static final int 기본_종료일_오프셋 = 7;
    private static final int 중간일_오프셋 = 6;

    // 사이트 사이즈 필터
    private static final String 대형_사이트 = "대형";
    private static final String 소형_사이트 = "소형";

    // =========================================================================
    // 검증 포인트
    // =========================================================================

    @Nested
    @DisplayName("검증 포인트")
    class 검증_포인트 {

        @Test
        @DisplayName("VP-01: 예약된 사이트는 가용 목록에서 제외")
        void 예약된_사이트는_가용_목록에서_제외된다() {
            // given - A1만 예약됨
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            LocalDate 조회일 = daysFromNow(중간일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 가용_사이트_목록_조회(조회일);

            // then - A1은 제외, A2/B1/B2는 포함
            응답_검증_성공(응답);
            List<String> 사이트목록 = 응답.jsonPath().getList("siteNumber", String.class);
            assertThat(사이트목록)
                    .contains(SITE_A2, SITE_B1, SITE_B2)
                    .doesNotContain(SITE_A1);
        }

        @Test
        @Disabled("BUG: 취소된 예약이 가용성 계산에서 제외되지 않음")
        @DisplayName("VP-02: 취소된 예약의 사이트는 가용으로 표시")
        void 취소된_예약의_사이트는_가용으로_표시된다() {
            // given - 취소된 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createCancelledReservation(SITE_A1, 시작일, 종료일);

            LocalDate 조회일 = daysFromNow(중간일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);

            // then - 취소된 예약은 가용성 계산에서 제외되어야 함
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getBoolean("available")).isTrue();
        }

        @Test
        @DisplayName("VP-03-1: 단일 날짜 조회 - 예약 없으면 available: true")
        void 예약이_없으면_가용으로_표시된다() {
            // given
            LocalDate 조회일 = daysFromNow(기본_시작일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
            assertThat(응답.jsonPath().getBoolean("available")).isTrue();
        }

        @Test
        @DisplayName("VP-03-2: 단일 날짜 조회 - 예약 있으면 available: false")
        void 예약이_있으면_불가용으로_표시된다() {
            // given - 5~7일에 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            LocalDate 조회일 = daysFromNow(중간일_오프셋); // 예약 기간 중간

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);

            // then
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getBoolean("available")).isFalse();
        }

        @Test
        @DisplayName("VP-03-3: 예약이 없으면 모든 사이트가 반환된다")
        void 예약이_없으면_모든_사이트가_반환된다() {
            // given
            LocalDate 조회일 = daysFromNow(기본_시작일_오프셋);

            // when
            ExtractableResponse<Response> 응답 = 가용_사이트_목록_조회(조회일);

            // then
            응답_검증_성공(응답);
            List<String> 사이트목록 = 응답.jsonPath().getList("siteNumber", String.class);
            assertThat(사이트목록).contains(SITE_A1, SITE_A2, SITE_B1, SITE_B2);
        }

        @Test
        @DisplayName("VP-04-1: 사이즈 필터 - 대형 사이트만 검색")
        void 대형_사이트만_검색할_수_있다() {
            // given
            LocalDate 검색시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 검색종료일 = daysFromNow(기본_종료일_오프셋);

            // when - size=대형 파라미터로 필터링
            ExtractableResponse<Response> 응답 = 사이트_검색(검색시작일, 검색종료일, 대형_사이트);

            // then - A로 시작하는 사이트만 반환
            응답_검증_성공(응답);
            List<String> 사이트목록 = 응답.jsonPath().getList("siteNumber", String.class);
            assertThat(사이트목록)
                    .contains(SITE_A1, SITE_A2)
                    .doesNotContain(SITE_B1, SITE_B2);
        }

        @Test
        @DisplayName("VP-04-2: 사이즈 필터 - 소형 사이트만 검색")
        void 소형_사이트만_검색할_수_있다() {
            // given
            LocalDate 검색시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 검색종료일 = daysFromNow(기본_종료일_오프셋);

            // when - size=소형 파라미터로 필터링
            ExtractableResponse<Response> 응답 = 사이트_검색(검색시작일, 검색종료일, 소형_사이트);

            // then - B로 시작하는 사이트만 반환
            응답_검증_성공(응답);
            List<String> 사이트목록 = 응답.jsonPath().getList("siteNumber", String.class);
            assertThat(사이트목록)
                    .contains(SITE_B1, SITE_B2)
                    .doesNotContain(SITE_A1, SITE_A2);
        }

        @Test
        @DisplayName("VP-05-1: 과거 날짜 조회 거부 - 단일 날짜 API")
        void 과거_날짜_조회는_거부된다() {
            // given
            LocalDate 과거날짜 = LocalDate.now().minusDays(5);

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 과거날짜);

            // then - RuntimeException → 500 에러
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        @DisplayName("VP-05-2: 존재하지 않는 사이트 조회 거부")
        void 존재하지_않는_사이트_조회는_거부된다() {
            // given
            LocalDate 조회일 = daysFromNow(기본_시작일_오프셋);
            String 존재하지않는사이트 = "Z99";

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(존재하지않는사이트, 조회일);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        @DisplayName("VP-05-3: 기간 검색 시 종료일이 시작일보다 이전이면 거부")
        void 기간_검색시_종료일이_시작일보다_이전이면_거부된다() {
            // given
            LocalDate 검색시작일 = daysFromNow(10);
            LocalDate 검색종료일 = daysFromNow(5);

            // when
            ExtractableResponse<Response> 응답 = 사이트_검색(검색시작일, 검색종료일, null);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // =========================================================================
    // 경계값 검증
    // =========================================================================

    @Nested
    @DisplayName("경계값 검증")
    class 경계값_검증 {

        @Test
        @DisplayName("BV-01-1: 조회일 = 오늘 허용")
        void 조회일이_오늘이면_허용된다() {
            // given
            LocalDate 오늘 = LocalDate.now();

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 오늘);

            // then
            응답_검증_성공(응답);
        }

        @Test
        @DisplayName("BV-01-2: 조회일 = 어제 거부")
        void 조회일이_어제면_거부된다() {
            // given
            LocalDate 어제 = LocalDate.now().minusDays(1);

            // when
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 어제);

            // then
            assertThat(응답.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        @DisplayName("BV-02: 예약 시작일 당일 조회 시 해당 사이트 제외")
        void 예약_시작일_당일_조회시_해당_사이트가_제외된다() {
            // given - 5~7일에 예약 생성
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

            // when - 예약 시작일에 가용성 조회
            ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 시작일);

            // then - 예약 시작일에는 해당 사이트가 unavailable
            응답_검증_성공(응답);
            assertThat(응답.jsonPath().getBoolean("available")).isFalse();
        }
    }

    // =========================================================================
    // API 일관성 검증
    // =========================================================================

    @Nested
    @DisplayName("API 일관성 검증")
    class API_일관성_검증 {

        @Test
        @DisplayName("단일 날짜 API와 가용 사이트 목록 API는 동일한 결과를 반환해야 한다")
        void 단일_날짜_API와_목록_API는_일관된_결과를_반환한다() {
            // given - A1에 10~12일 예약
            LocalDate 기존시작일 = daysFromNow(10);
            LocalDate 기존종료일 = daysFromNow(12);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            LocalDate 조회일 = daysFromNow(11); // 예약 기간 중간

            // when - 단일 날짜 API
            ExtractableResponse<Response> 단일응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);

            // when - 가용 사이트 목록 API
            ExtractableResponse<Response> 목록응답 = 가용_사이트_목록_조회(조회일);

            // then - 두 API 결과가 일관되어야 함
            boolean 단일가용여부 = 단일응답.jsonPath().getBoolean("available");
            List<String> 가용사이트목록 = 목록응답.jsonPath().getList("siteNumber", String.class);

            // 단일 API에서 unavailable이면, 목록 API에서도 해당 사이트가 없어야 함
            assertThat(단일가용여부).isFalse();
            assertThat(가용사이트목록).doesNotContain(SITE_A1);
        }

        @Test
        @Disabled("BUG: API 간 쿼리 불일치 - existsByCampsiteAndReservationDate vs 기간 쿼리")
        @DisplayName("모든 API가 동일한 날짜에 대해 일관된 결과를 반환해야 한다")
        void 모든_API가_일관된_결과를_반환해야_한다() {
            // given - A1에 10~12일 예약
            LocalDate 기존시작일 = daysFromNow(10);
            LocalDate 기존종료일 = daysFromNow(12);
            testDataFactory.createReservation(SITE_A1, 기존시작일, 기존종료일, "김철수", "01098765432");

            LocalDate 조회일 = daysFromNow(11);

            // when - 세 가지 API 호출
            ExtractableResponse<Response> 단일응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);
            ExtractableResponse<Response> 목록응답 = 가용_사이트_목록_조회(조회일);
            ExtractableResponse<Response> 검색응답 = 사이트_검색(조회일, 조회일, null);

            // then - 모든 API에서 A1이 unavailable이어야 함
            assertThat(단일응답.jsonPath().getBoolean("available")).isFalse();
            assertThat(목록응답.jsonPath().getList("siteNumber", String.class)).doesNotContain(SITE_A1);
            assertThat(검색응답.jsonPath().getList("siteNumber", String.class)).doesNotContain(SITE_A1);
        }
    }

    // =========================================================================
    // 헬퍼 메서드
    // =========================================================================

    private ExtractableResponse<Response> 단일_사이트_가용성_조회(String siteNumber, LocalDate date) {
        return given()
                .queryParam("date", date.toString())
                .when()
                .get("/api/sites/{siteNumber}/availability", siteNumber)
                .then()
                .extract();
    }

    private ExtractableResponse<Response> 가용_사이트_목록_조회(LocalDate date) {
        return given()
                .queryParam("date", date.toString())
                .when()
                .get("/api/sites/available")
                .then()
                .extract();
    }

    private ExtractableResponse<Response> 사이트_검색(LocalDate startDate, LocalDate endDate, String size) {
        var request = given()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString());

        if (size != null) {
            request.queryParam("size", size);
        }

        return request
                .when()
                .get("/api/sites/search")
                .then()
                .extract();
    }

    // =========================================================================
    // Assertion 헬퍼
    // =========================================================================

    private void 응답_검증_성공(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }
}
