package com.camping.acceptance.site;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

public class SiteSteps {

    public static ExtractableResponse<Response> 사이트_검색_요청(LocalDate startDate, LocalDate endDate) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트_검색_요청(LocalDate startDate, LocalDate endDate, String size) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", size)
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트_검색_요청_날짜없이() {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }
}
