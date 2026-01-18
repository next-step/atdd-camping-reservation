package com.camping.legacy.step;

import static io.restassured.RestAssured.given;
import static java.time.temporal.TemporalAdjusters.*;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class ReservationStep {
    private static final String RESERVATION_ENDPOINT = "/api/reservations";

    public static ExtractableResponse<Response> 예약을_요청한다(ReservationRequest request) {
        return given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_조회한다(long reservationId) {
        return  given().log().all()
                .queryParam("id", reservationId)
                .when().get(RESERVATION_ENDPOINT)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_취소한다(long reservationId, String confirmationCode) {
        return given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete(RESERVATION_ENDPOINT + "/" + reservationId)
                .then().log().all()
                .extract();
    }
}
