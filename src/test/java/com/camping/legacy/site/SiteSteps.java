package com.camping.legacy.site;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteSteps {

    public static ExtractableResponse<Response> 전체_사이트_목록_조회_요청() {
        return given().log().all()
                .when().get("/api/sites")
                .then().log().all().extract();
    }

    public static void A구역_대형_사이트들이_반환된다(ExtractableResponse<Response> response) {
        List<String> aSites = response.jsonPath().getList("findAll { it.siteNumber.startsWith('A') }.siteNumber");
        List<String> aSiteSizes = response.jsonPath().getList("findAll { it.siteNumber.startsWith('A') }.size");

        assertThat(aSites.size()).isGreaterThan(0);
        assertThat(aSiteSizes).allMatch(size -> "대형".equals(size));
    }

    public static void B구역_소형_사이트들이_반환된다(ExtractableResponse<Response> response) {
        List<String> bSites = response.jsonPath().getList("findAll { it.siteNumber.startsWith('B') }.siteNumber");
        List<String> bSiteSizes = response.jsonPath().getList("findAll { it.siteNumber.startsWith('B') }.size");

        assertThat(bSites.size()).isGreaterThan(0);
        assertThat(bSiteSizes).allMatch(size -> "소형".equals(size));
    }

    public static void 각_사이트의_최대_수용_인원이_포함된다(ExtractableResponse<Response> response) {
        List<Integer> maxPeopleList = response.jsonPath().getList("maxPeople", Integer.class);

        assertThat(maxPeopleList).isNotEmpty();
        assertThat(maxPeopleList).allMatch(maxPeople -> maxPeople != null && maxPeople > 0);
    }

    public static ExtractableResponse<Response> 특정_날짜_가용_사이트_조회_요청(String date) {
        return given().log().all()
                .when().get("/api/sites/available?date=" + date)
                .then().log().all().extract();
    }

    public static void 사이트_A1에_예약이_존재한다(String date) {
        var reservationRequest = Map.of(
                "siteNumber", "A-1",
                "startDate", date,
                "endDate", date,
                "customerName", "테스트고객",
                "phoneNumber", "010-1234-5678"
        );

        var response = given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all().extract();

        assertThat(response.statusCode()).isEqualTo(201);
    }

    public static void 사이트_A2는_예약이_없다() {
    }

    public static void A2_사이트가_가용_사이트로_반환된다(ExtractableResponse<Response> response) {
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).contains("A-2");
    }

    public static void A1_사이트는_반환되지_않는다(ExtractableResponse<Response> response) {
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).doesNotContain("A-1");
    }

    public static void 사이트_A1에_기간_예약이_존재한다(String startDate, String endDate) {
        var reservationRequest = Map.of(
                "siteNumber", "A-1",
                "startDate", startDate,
                "endDate", endDate,
                "customerName", "테스트고객",
                "phoneNumber", "010-1234-5678"
        );

        var response = given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all().extract();

        assertThat(response.statusCode()).isEqualTo(201);
    }

    public static ExtractableResponse<Response> 기간별_가용_사이트_검색_요청(String startDate, String endDate) {
        return given().log().all()
                .when().get("/api/sites/search?startDate=" + startDate + "&endDate=" + endDate)
                .then().log().all().extract();
    }

    public static void A2_사이트만_반환된다(ExtractableResponse<Response> response) {
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).hasSize(1);
        assertThat(availableSites).contains("A-2");
    }

    public static void A1_사이트는_반환되지_않는다_기간검색(ExtractableResponse<Response> response) {
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).doesNotContain("A-1");
    }

    /**
     * 사이트들은 이미 데이터베이스에 초기화된 것으로 가정. 혹은 생성 API를 통해 생성.
     */
    public static void A구역_대형_사이트들과_B구역_소형_사이트들이_존재한다() {

    }

    public static ExtractableResponse<Response> 대형_사이트만_필터링하여_조회_요청() {
        return given().log().all()
                .when().get("/api/sites/search?size=대형")
                .then().log().all().extract();
    }

    public static void A로_시작하는_사이트들만_반환된다(ExtractableResponse<Response> response) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber", String.class);

        assertThat(siteNumbers).isNotEmpty();
        assertThat(siteNumbers).allMatch(siteNumber -> siteNumber.startsWith("A"));

        List<String> sizes = response.jsonPath().getList("size", String.class);
        assertThat(sizes).allMatch(size -> "대형".equals(size));
    }

    public static void B로_시작하는_사이트들은_반환되지_않는다(ExtractableResponse<Response> response) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber", String.class);

        assertThat(siteNumbers).noneMatch(siteNumber -> siteNumber.startsWith("B"));
    }
}
