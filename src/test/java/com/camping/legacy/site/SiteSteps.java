package com.camping.legacy.site;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class SiteSteps {

    public static ExtractableResponse<Response> 전체_사이트_목록_조회_요청() {
        return given().log().all()
                .when().get("/sites")
                .then().log().all().extract();
    }
}
