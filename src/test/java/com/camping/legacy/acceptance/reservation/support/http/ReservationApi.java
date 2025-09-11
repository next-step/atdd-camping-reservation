package com.camping.legacy.acceptance.reservation.support.http;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

public class ReservationApi {

    private ReservationApi() {
    }

    public static Response post(Object requestDto) {
        return RestAssured.given()
                .baseUri("http://localhost")
                .contentType(ContentType.JSON)
                .body(requestDto)
                .when()
                .post("/api/reservations")
                .then()
                .extract()
                .response();
    }

    public static void post(Object requestDto, HttpStatus status) {
        RestAssured.given()
                .baseUri("http://localhost")
                .contentType(ContentType.JSON)
                .body(requestDto)
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(status.value());
    }

    public static void delete(Long reservationId, String confirmationCode, HttpStatus status) {
        RestAssured.given()
                .baseUri("http://localhost")
                .when()
                .delete("/api/reservations/{id}?confirmationCode={code}", reservationId, confirmationCode)
                .then()
                .statusCode(status.value());
    }
}
