package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

public class ReservationHelper {

    public static final String RESERVATION_BASE_URL = "/api/reservations";

    static ExtractableResponse<Response> 예약_생성_요청(ReservationRequest request, HttpStatus status) {
        return RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post(RESERVATION_BASE_URL)
                .then().log().all()
                    .statusCode(status.value())
                    .extract();
    }

    static ExtractableResponse<Response> 예약_취소_요청(String confirmationCode, Long reservationId, HttpStatus status) {
        return RestAssured
                .given().log().all()
                    .queryParam("confirmationCode", confirmationCode)
                .when()
                    .delete(RESERVATION_BASE_URL + "/" + reservationId)
                .then().log().all()
                    .statusCode(status.value())
                    .extract();
    }
}
