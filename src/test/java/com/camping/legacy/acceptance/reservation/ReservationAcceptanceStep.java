package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class ReservationAcceptanceStep {
    final static String BASE_URL = "/api/reservations";

    public static ReservationResponse 예약_생성_성공(ReservationRequest request) {
        return 예약_생성_요청(request, 201).as(ReservationResponse.class);
    }

    static ExtractableResponse<Response> 예약_생성_요청(ReservationRequest request, Integer statusCode) {
        return RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post(BASE_URL)
                .then()
                    .statusCode(statusCode)
                    .extract();
    }
}
