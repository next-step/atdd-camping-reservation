package com.camping.legacy.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationTestHelper {

    public static void assertReservationSuccess(ExtractableResponse<Response> extract, Map<String, Object> expected) {
        assertThat(extract.jsonPath().getString("status")).isEqualTo("CONFIRMED");
        assertThat(extract.jsonPath().getString("confirmationCode")).matches("^[A-Z0-9]{6}$");
        assertThat(extract.jsonPath().getString("customerName")).isEqualTo(expected.get("customerName"));
        assertThat(extract.jsonPath().getString("siteNumber")).isEqualTo(expected.get("siteNumber"));
        assertThat(extract.jsonPath().getString("phoneNumber")).isEqualTo(expected.get("phoneNumber"));
        assertThat(extract.jsonPath().getString("startDate")).isEqualTo(expected.get("startDate"));
        assertThat(extract.jsonPath().getString("endDate")).isEqualTo(expected.get("endDate"));
    }

    public static void sendCancelRequest(String confirmationCode, Long reservationId) {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .log().all()
                .when()
                .delete("/api/reservations/{id}", reservationId)
                .then()
                .log().all()
                .extract();
    }

    public static ExtractableResponse<Response> sendReservationCreateRequest(Map<String, Object> request) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();
    }

    public static ExtractableResponse<Response> sendReservationEditRequest(
            Long reservationId,
            String confirmationCode,
            Map<String, Object> request
    ) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body(request)
                .log().all()
                .when()
                .put("/api/reservations/{id}", reservationId)
                .then()
                .log().all()
                .extract();
    }
}
