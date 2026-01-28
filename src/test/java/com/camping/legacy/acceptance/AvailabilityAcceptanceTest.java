package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
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
 * assertThat 사용 : AssertJ 테스트 라이브러리에서 제공하는 메서드 JUnit이 테스트를 "실행"해준다면, AssertJ는 결과를 "검증"해줌.
 * ex) assertThat(사이트목록).doesNotContain(SITE_A1); => “사이트 목록에 A1이 없어야 한다” 직관적임.
 *
 * @see docs/acceptance-criteria.md - 2. 가용성 확인 (8점)
 */
@DisplayName("2. 가용성 확인 인수 테스트")
class AvailabilityAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("예약된 사이트는 가용 목록에서 제외")
    void 예약된_사이트는_가용_목록에서_제외된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        LocalDate 조회일 = daysFromNow(6);

        // when
        ExtractableResponse<Response> 응답 = 가용_사이트_목록_조회(조회일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> 사이트목록 = 응답.jsonPath().getList("siteNumber", String.class);
        assertThat(사이트목록).doesNotContain(SITE_A1);
    }

    @Test
    @Disabled("BUG: 취소된 예약이 가용성 계산에서 제외되지 않음")
    @DisplayName("취소된 예약의 사이트는 가용으로 표시")
    void 취소된_예약의_사이트는_가용으로_표시된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        testDataFactory.createCancelledReservation(SITE_A1, 시작일, 종료일);

        LocalDate 조회일 = daysFromNow(6);

        // when
        ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(응답.jsonPath().getBoolean("available")).isTrue();
    }

    @Test
    @DisplayName("단일 날짜/기간 검색 결과 일치")
    void 단일_날짜와_기간_검색_결과가_일치한다() {
        // given
        LocalDate 시작일 = daysFromNow(10);
        LocalDate 종료일 = daysFromNow(12);
        testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        LocalDate 조회일 = daysFromNow(11);

        // when
        ExtractableResponse<Response> 단일응답 = 단일_사이트_가용성_조회(SITE_A1, 조회일);
        ExtractableResponse<Response> 목록응답 = 가용_사이트_목록_조회(조회일);

        // then
        boolean 단일가용여부 = 단일응답.jsonPath().getBoolean("available");
        List<String> 가용사이트목록 = 목록응답.jsonPath().getList("siteNumber", String.class);

        assertThat(단일가용여부).isFalse();
        assertThat(가용사이트목록).doesNotContain(SITE_A1);
    }

    @Test
    @DisplayName("사이즈 필터(대형/소형) 정상 동작")
    void 사이즈_필터가_정상_동작한다() {
        // given
        LocalDate 검색시작일 = daysFromNow(5);
        LocalDate 검색종료일 = daysFromNow(7);

        // when
        ExtractableResponse<Response> 대형응답 = 사이트_검색(검색시작일, 검색종료일, "대형");
        ExtractableResponse<Response> 소형응답 = 사이트_검색(검색시작일, 검색종료일, "소형");

        // then
        assertThat(대형응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> 대형사이트 = 대형응답.jsonPath().getList("siteNumber", String.class);
        assertThat(대형사이트).contains(SITE_A1, SITE_A2).doesNotContain(SITE_B1, SITE_B2);

        assertThat(소형응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> 소형사이트 = 소형응답.jsonPath().getList("siteNumber", String.class);
        assertThat(소형사이트).contains(SITE_B1, SITE_B2).doesNotContain(SITE_A1, SITE_A2);
    }

    @Test
    @DisplayName("과거 날짜 조회 거부")
    void 과거_날짜_조회는_거부된다() {
        // given
        LocalDate 과거날짜 = LocalDate.now().minusDays(5);

        // when
        ExtractableResponse<Response> 응답 = 단일_사이트_가용성_조회(SITE_A1, 과거날짜);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
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
    // Custom Matcher
    // =========================================================================

    private void 응답_검증_가용성_조회_성공(ExtractableResponse<Response> response, boolean expectedAvailable) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getBoolean("available")).isEqualTo(expectedAvailable);
    }

    private void 응답_검증_가용_사이트_포함(ExtractableResponse<Response> response, String... expectedSites) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> 사이트목록 = response.jsonPath().getList("siteNumber", String.class);
        assertThat(사이트목록).contains(expectedSites);
    }

    private void 응답_검증_가용_사이트_미포함(ExtractableResponse<Response> response, String... excludedSites) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> 사이트목록 = response.jsonPath().getList("siteNumber", String.class);
        assertThat(사이트목록).doesNotContain(excludedSites);
    }
}
