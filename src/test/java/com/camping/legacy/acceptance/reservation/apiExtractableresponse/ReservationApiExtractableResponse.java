package com.camping.legacy.acceptance.reservation.apiExtractableresponse;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

public class ReservationApiExtractableResponse {

    public static ExtractableResponse<Response> 예약을_생성한다(ReservationRequest request) {
        return RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_수정한다(Long id, String confirmationCode, ReservationRequest request) {
        return RestAssured.given().log().all()
                .pathParam("id", id)
                .queryParam("confirmationCode", confirmationCode)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().put("/api/reservations/{id}")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_취소한다(Long id, String confirmationCode) {
        return RestAssured.given().log().all()
                .pathParam("id", id)
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/{id}")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_조회한다(Long id) {
        return RestAssured.given().log().all()
                .pathParam("id", id)
                .when().get("/api/reservations/{id}")
                .then().log().all()
                .extract();
    }

}
