package com.camping.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

public class SiteSteps {

    public static final String API_SITES_SEARCH = "/api/sites/search";
    public static final String API_SITES = "/api/sites";

    public static ExtractableResponse<Response> 예약_가능_사이트를_검색한다(LocalDate startDate, LocalDate endDate, String size) {
        return RestAssured
                .given().log().all()
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("size", size)
                .when()
                .get(API_SITES_SEARCH)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트_상세_정보를_조회한다(Long siteId) {
        return RestAssured
                .given().log().all()
                .when()
                .get(API_SITES + "/" + siteId)
                .then().log().all()
                .extract();
    }
}
