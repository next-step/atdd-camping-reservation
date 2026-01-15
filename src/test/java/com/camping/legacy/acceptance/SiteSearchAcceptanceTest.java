package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class SiteSearchAcceptanceTest extends AcceptanceTestBase {

    private static final String RESERVATIONS_API = "/api/reservations";
    private static final String SITE_AVAILABILITY_API_TEMPLATE = "/api/sites/%s/availability";
    private static final String AVAILABLE_SITES_API = "/api/sites/available";
    private static final String SEARCH_SITES_API = "/api/sites/search";

    @Test
    void 예약_가능_여부_조회에서_예약_기간에_포함되면_available_false() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        createReservation("홍길동", "A-1", start, end);

        // When
        ExtractableResponse<Response> response = given()
                .accept(ContentType.JSON)
                .queryParam("date", start.plusDays(1).toString())
                .when()
                .get(String.format(SITE_AVAILABILITY_API_TEMPLATE, "A-1"))
                .then()
                .extract();

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-1");
    }

    @Test
    void 특정_날짜에_예약_가능한_사이트_목록을_반환한다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        createReservation("홍길동", "A-1", start, end);

        // When
        ExtractableResponse<Response> response = given()
                .accept(ContentType.JSON)
                .queryParam("date", start.toString())
                .when()
                .get(AVAILABLE_SITES_API)
                .then()
                .extract();

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        // 예약된 사이트는 제외된다
        assertThat(response.jsonPath().getList("siteNumber")).doesNotContain("A-1");

        // 예약 가능한 사이트도 존재한다
        assertThat(response.jsonPath().getList("siteNumber")).contains("B-1");
    }

    // TODO api 좀 파악하기
    @Test
    void 검색_결과에는_기간_내_모든_날짜가_예약_가능한_사이트만_포함된다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        createReservation("홍길동", "A-1", start, end);

        // When
        ExtractableResponse<Response> response = given()
                .accept(ContentType.JSON)
                .queryParam("startDate", start.toString())
                .queryParam("endDate", end.toString())
                .when()
                .get(SEARCH_SITES_API)
                .then()
                .extract();

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("siteNumber")).doesNotContain("A-1");
    }

    // FIXME 에러 문구 처리되도록 수정 필요
    @Disabled
    @Test
    void 예외_과거_날짜로_기간_검색을_시도하면_실패한다() {
        // Given: 오늘 기준 과거 날짜로 요청
        LocalDate start = LocalDate.now().minusDays(3);
        LocalDate end = LocalDate.now().minusDays(1);

        // When
        ExtractableResponse<Response> response = given()
                .accept(ContentType.JSON)
                .queryParam("startDate", start.toString())
                .queryParam("endDate", end.toString())
                .when()
                .get(SEARCH_SITES_API)
                .then()
                .extract();

        // Then
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.asString()).contains("과거 날짜는 검색할 수 없습니다.");
    }

    // FIXME 에러 문구 처리되도록 수정 필요
    @Disabled
    @Test
    void 예외_종료일이_시작일보다_이전이면_기간_검색이_실패한다() {
        LocalDate start = LocalDate.now().plusDays(12);
        LocalDate end = LocalDate.now().plusDays(10);

        ExtractableResponse<Response> response = given()
                .accept(ContentType.JSON)
                .queryParam("startDate", start.toString())
                .queryParam("endDate", end.toString())
                .when()
                .get(SEARCH_SITES_API)
                .then()
                .extract();

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.asString()).contains("종료일이 시작일보다 이전일 수 없습니다.");
    }

    private ExtractableResponse<Response> createReservation(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return given()
                .contentType(ContentType.JSON)
                .body(new ReservationRequestBody(
                        customerName,
                        startDate.toString(),
                        endDate.toString(),
                        siteNumber,
                        "010-0000-0000",
                        2,
                        "12가3456",
                        "요청사항 없음"
                ))
                .when()
                .post(RESERVATIONS_API)
                .then()
                .extract();
    }

    private record ReservationRequestBody(
            String customerName,
            String startDate,
            String endDate,
            String siteNumber,
            String phoneNumber,
            Integer numberOfPeople,
            String carNumber,
            String requests
    ) {
    }
}
