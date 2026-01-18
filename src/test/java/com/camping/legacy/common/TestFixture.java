package com.camping.legacy.common;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

public class TestFixture {

    // ==================== 사이트 관련 ====================

    public static ExtractableResponse<Response> 전체_사이트_조회_요청() {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트_조회_요청(Long siteId) {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites/{siteId}", siteId)
                .then().log().all()
                .extract();
    }

    public static Long 사이트_ID_조회(String siteNumber) {
        ExtractableResponse<Response> response = 전체_사이트_조회_요청();
        return response.jsonPath().getLong("find { it.siteNumber == '" + siteNumber + "' }.id");
    }

    // ==================== 예약 관련 ====================

    public static ExtractableResponse<Response> 예약_생성_요청(
            String siteNumber, String startDate, String endDate,
            String customerName, int numberOfPeople) {

        Map<String, Object> params = new HashMap<>();
        params.put("siteNumber", siteNumber);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("customerName", customerName);
        params.put("numberOfPeople", numberOfPeople);
        params.put("phoneNumber", "010-1234-5678");

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_수정_요청(
            Long reservationId, String confirmationCode,
            String startDate, String endDate) {

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("confirmationCode", confirmationCode)
                .body(params)
                .when().put("/api/reservations/{id}", reservationId)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_취소_요청(Long id, String confirmationCode) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/{id}", id)
                .then().log().all()
                .extract();
    }

    // ==================== 캘린더 관련 ====================

    public static ExtractableResponse<Response> 캘린더_조회_요청(Long siteId, int year, int month) {
        return RestAssured
                .given().log().all()
                .queryParam("siteId", siteId)
                .queryParam("year", year)
                .queryParam("month", month)
                .when().get("/api/reservations/calendar")
                .then().log().all()
                .extract();
    }
}