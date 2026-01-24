package com.camping.legacy.client;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

public class SiteClient {

    public static ExtractableResponse<Response> 전체_사이트_조회_API() {
        return RestAssured.given()
                .when()
                .get("/api/sites")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트_상세_조회_API(Long siteId) {
        return RestAssured.given()
                .when()
                .get("/api/sites/{siteId}", siteId)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> 가용_사이트_조회_API(LocalDate date) {
        return RestAssured.given()
                .param("date", date.toString())
                .when()
                .get("/api/sites/available")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> 기간별_가용_사이트_검색_API(LocalDate startDate, LocalDate endDate) {
        return RestAssured.given()
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .when()
                .get("/api/sites/search")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트_가용성_확인_API(String siteNumber, LocalDate date) {
        return RestAssured.given()
                .param("date", date.toString())
                .when()
                .get("/api/sites/{siteNumber}/availability", siteNumber)
                .then()
                .extract();
    }
}
