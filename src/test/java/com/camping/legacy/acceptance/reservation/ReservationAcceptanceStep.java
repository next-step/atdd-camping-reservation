package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

public class ReservationAcceptanceStep {
    final static String BASE_URL = "/api/reservations";

    public static ReservationRequest getReservationRequest(
            String customerName,
            LocalDate startDate,
            LocalDate endDate,
            String siteNumber,
            String phone
    ) {
        return new ReservationRequest(
                customerName,
                startDate,
                endDate,
                siteNumber,
                phone,
                2,
                null,
                null
        );
    }

    public static ReservationRequest getReservationRequest(int index, LocalDate startDate, LocalDate endDate, String siteNumber) {
        return getReservationRequest(
                "고객" + index,
                startDate,
                endDate,
                siteNumber,
                "010-1234-567" + index
        );
    }

    public static ReservationResponse 예약_생성_성공(ReservationRequest request) {

        return 예약_생성_요청(request, 201).as(ReservationResponse.class);
    }

    public static String 예약_생성_실패(ReservationRequest request, Integer statusCode) {
        return 예약_생성_요청(request, statusCode).jsonPath().getString("message");
    }

    static ExtractableResponse<Response> 예약_생성_요청(ReservationRequest request, Integer statusCode) {
        return RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post(BASE_URL)
                .then()
                    .log().all()
                    .statusCode(statusCode)
                    .extract();
    }

    public static String 예약_취소_성공(Long id, String confirmationCode) {
        return 예약_취소_요청(id, confirmationCode, 200).jsonPath().getString("message");
    }

    static ExtractableResponse<Response> 예약_취소_요청(Long id, String confirmationCode, Integer statusCode) {
        return RestAssured
                .given()
                    .log().all()
                    .param("confirmationCode", confirmationCode)
                .when()
                    .delete(BASE_URL + "/" + id)
                .then()
                    .log().all()
                    .statusCode(statusCode)
                    .extract();
    }
}
