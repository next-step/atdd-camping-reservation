package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class ReservationSteps {

    public static final String RESERVATIONS_URL = "/api/reservations";

    public static ExtractableResponse<Response> createReservation(
            String customerName,
            String siteNumber,
            int startDaysFromNow,
            int endDaysFromNow
    ) {
        var request = createReservationRequest(customerName, siteNumber, startDaysFromNow, endDaysFromNow);
        return given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post(RESERVATIONS_URL)
                .then()
                    .statusCode(201) // given 단계 내에서 검증
                    .extract();
    }

    public static ExtractableResponse<Response> createReservationExpectingFailure(
            String customerName,
            String siteNumber,
            int startDaysFromNow,
            int endDaysFromNow
    ) {
        var request = createReservationRequest(customerName, siteNumber, startDaysFromNow, endDaysFromNow);

        return given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post(RESERVATIONS_URL)
                .then()
                   .extract();
    }

    public static ExtractableResponse<Response> cancelReservation(Long reservationId, String confirmationCode) {
        return given()
                    .queryParam("confirmationCode", confirmationCode)
                .when()
                    .delete(RESERVATIONS_URL + "/{id}", reservationId)
                .then()
                    .extract();
    }

    public static ExtractableResponse<Response> getReservation(Long reservationId) {
        return given()
                .when()
                    .get(RESERVATIONS_URL + "/{id}", reservationId)
                .then()
                    .extract();
    }

    public static ExtractableResponse<Response> updateReservation(Long reservationId, String confirmationCode, Map<String, Object> updateRequest) {
        return given()
                    .contentType(ContentType.JSON)
                    .queryParam("confirmationCode", confirmationCode)
                .body(updateRequest)
                .when()
                    .put(RESERVATIONS_URL + "/{id}", reservationId)
                .then()
                    .extract();
    }

    private static Map<String, Object> createReservationRequest(
            String customerName,
            String siteNumber,
            int startDaysFromNow,
            int endDaysFromNow
    ) {
        var request = new HashMap<String, Object>();
        request.put("customerName", customerName);
        request.put("phoneNumber", "01012345678");
        request.put("siteNumber", siteNumber);
        request.put("startDate", LocalDate.now().plusDays(startDaysFromNow).toString());
        request.put("endDate", LocalDate.now().plusDays(endDaysFromNow).toString());
        return request;
    }
}
