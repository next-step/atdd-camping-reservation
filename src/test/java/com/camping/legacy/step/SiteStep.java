package com.camping.legacy.step;

import static io.restassured.RestAssured.given;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;

public class SiteStep {
    private static final String SITE_ENDPOINT = "/api/sites";

    public static ExtractableResponse<Response> 기간_조건으로_사이트를_검색한다(LocalDate startDate, LocalDate endDate) {
        return given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트를_검색한다(LocalDate startDate, LocalDate endDate, String size) {
        return given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", size)
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .extract();
    }
}
