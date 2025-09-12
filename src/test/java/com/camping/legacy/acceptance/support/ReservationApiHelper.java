package com.camping.legacy.acceptance.support;

import static io.restassured.RestAssured.given;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.Map;

public class ReservationApiHelper {

    public static ExtractableResponse<Response> createReservation(Map<String, Object> reservationData) {
        return given()
                .contentType("application/json")
                .body(reservationData)
                .when()
                    .post("/api/reservations")
                .then()
                    .extract();
    }

    public static ExtractableResponse<Response> checkSiteAvailability(String siteNumber, LocalDate date) {
        return given()
                .when()
                    .get("/api/sites/" + siteNumber + "/availability?date=" + date)
                .then()
                    .extract();
    }

    public static ExtractableResponse<Response> createReservationAndExpectSuccess(Map<String, Object> reservationData) {
        return createReservation(reservationData);
    }

    public static ExtractableResponse<Response> createExistingReservation(String siteNumber, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
                .withSiteNumber(siteNumber)
                .withDates(startDate, endDate)
                .build();
        
        return createReservation(existingReservation);
    }

    public static ExtractableResponse<Response> checkSiteAvailabilityWithoutDateParam(String siteNumber) {
        return given()
                .when()
                    .get("/api/sites/" + siteNumber + "/availability")
                .then()
                    .extract();
    }
}