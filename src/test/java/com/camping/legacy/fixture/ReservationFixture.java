package com.camping.legacy.fixture;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationFixture {

    public static ReservationRequest createRequest(String customerName, String siteNumber,
        LocalDate startDate, LocalDate endDate) {
        return new ReservationRequest(
            customerName,
            startDate,
            endDate,
            siteNumber,
            "01012345678",
            2,
            "12가3456",
            null
        );
    }

    public static ReservationRequest createConcurrencyRequest(int index, String siteNumber,
        LocalDate startDate, LocalDate endDate) {
        return new ReservationRequest(
            "고객" + index,
            startDate,
            endDate,
            siteNumber,
            "0101234567" + index,
            2,
            null,
            null
        );
    }

    public static ExtractableResponse<Response> createReservation(ReservationRequest request) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/reservations")
            .then()
            .extract();
    }

    public static ReservationResponse createReservationAndVerify(ReservationRequest request) {
        var response = createReservation(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return response.as(ReservationResponse.class);
    }

    public static ExtractableResponse<Response> cancelReservation(Long reservationId, String confirmationCode) {
        return RestAssured.given()
            .param("confirmationCode", confirmationCode)
            .when()
            .delete("/api/reservations/{id}", reservationId)
            .then()
            .extract();
    }

    public static ExtractableResponse<Response> getReservation(Long reservationId) {
        return RestAssured.given()
            .when()
            .get("/api/reservations/{id}", reservationId)
            .then()
            .extract();
    }
}
