package com.camping.acceptance.sample;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.util.Map;

public class SampleSteps {

    public static ExtractableResponse<Response> 생성_요청(Map<String, Object> request) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/api/samples")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 조회_요청(Long id) {
        return RestAssured
                .given().log().all()
                .when().get("/api/samples/{id}", id)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 수정_요청(Long id, Map<String, Object> request) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().put("/api/samples/{id}", id)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 삭제_요청(Long id) {
        return RestAssured
                .given().log().all()
                .when().delete("/api/samples/{id}", id)
                .then().log().all()
                .extract();
    }
}