package com.camping.legacy.domain;

import com.camping.legacy.domain.dto.ReservationParams;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

public class RequestSender {

    static ExtractableResponse<Response> get(String path, Map<String, String> params) {
        return RestAssured
                .given()
                    .log().all()
                .contentType(ContentType.JSON)
                    .queryParams(params)
                .when()
                    .get(path)
                .then()
                    .log().all()
                    .extract();
    }

    static ExtractableResponse<Response> post(String path, Map<String, String> params) {
        return RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(params)
                .when()
                .post(path)
                .then()
                .log().all()
                .extract();
    }
}
