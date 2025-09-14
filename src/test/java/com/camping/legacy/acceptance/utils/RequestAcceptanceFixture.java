package com.camping.legacy.acceptance.utils;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

public class RequestAcceptanceFixture {
    static final String RESERVATION_PATH = "/api/reservations";

    public static ExtractableResponse<Response> createReservation(Map<String, String> requestData) {
        return RestAssured
                .given()
                    .body(requestData)
                .when()
                    .post(RESERVATION_PATH)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> updateReservation(Long id, String confirmationCode, Map<String, String> requestData) {
        return RestAssured
                .given()
                    .body(requestData)
                .when()
                    .put(RESERVATION_PATH + "/" + id + "?confirmationCode=" + confirmationCode)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> cancelReservation(Long id, String confirmationCode) {
        return RestAssured
                .given()
                .when()
                    .delete(RESERVATION_PATH + "/" + id + "?confirmationCode=" + confirmationCode)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> getReservation(Long id) {
        return RestAssured
                .given()
                .when()
                    .get(RESERVATION_PATH + "/" + id)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> getReservations() {
        return RestAssured
                .given()
                .when()
                    .get(RESERVATION_PATH + "/" )
                .then()
                .extract();
    }
}
