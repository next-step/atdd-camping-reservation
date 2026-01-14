package com.camping.legacy.acceptance.reservation;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class SiteApiExtractableResponse {

    public static ExtractableResponse<Response> 사이트를_조회한다(String startDate, String endDate, String size) {
        return RestAssured.given().log().all()
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .queryParam("size", size)
                .when().get("/api/sites/search")
                .then().log().all()
                .statusCode(200)
                .extract();
    }

}
