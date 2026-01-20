package com.camping.legacy.client;

import static io.restassured.RestAssured.given;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;

public class SiteClient {
    private static final String SITE_ENDPOINT = "/api/sites";

    public static ExtractableResponse<Response> 기간_조건으로_사이트를_검색한다(int startDayOffset, int endDayOffset) {
        var startDate = LocalDate.now().plusDays(startDayOffset);
        var endDate = LocalDate.now().plusDays(endDayOffset);
        return given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트를_검색한다(int startDayOffset, int endDayOffset, String size) {
        var startDate = LocalDate.now().plusDays(startDayOffset);
        var endDate = LocalDate.now().plusDays(endDayOffset);
        return given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", size)
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .statusCode(200)
                .extract();
    }
}
