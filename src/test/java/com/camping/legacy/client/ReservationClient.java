package com.camping.legacy.client;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class ReservationClient {
    
    public static ExtractableResponse<Response> 예약_생성_API(ReservationRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_취소_API(Long reservationId, String confirmationCode) {
        return RestAssured.given()
                .param("confirmationCode", confirmationCode)
                .when()
                .delete("/api/reservations/{id}", reservationId)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_조회_API(Long reservationId) {
        return RestAssured.given()
                .when()
                .get("/api/reservations/{id}", reservationId)
                .then()
                .extract();
    }
}
