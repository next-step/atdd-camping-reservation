package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class AcceptanceTestFixTure {
    public static ReservationRequest createReservationRequest() {
        LocalDate now = LocalDate.now();
        return new ReservationRequest(
                "김영희",
                now.plusDays(20),
                now.plusDays(20),
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createSameReservationRequest() {
        LocalDate now = LocalDate.now();
        return new ReservationRequest(
                "박철수",
                now.plusDays(20),
                now.plusDays(20),
                "A-1",
                "010-9876-5432",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createCancelledReservationRequest() {
        LocalDate now = LocalDate.now();
        return new ReservationRequest(
                "김영희",
                now.plusDays(20),
                now.plusDays(20),
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createCancelledSameReservationRequest() {
        LocalDate now = LocalDate.now();
        return new ReservationRequest(
                "박철수",
                now.plusDays(20),
                now.plusDays(20),
                "A-1",
                "010-9876-5432",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createWrongReservationRequest() {
        LocalDate now = LocalDate.now();
        return new ReservationRequest(
                "김영희",
                now.plusDays(30),
                now.plusDays(30),
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createBookedReservationRequest() {
        LocalDate now = LocalDate.now();
        return new ReservationRequest(
                "김영희",
                now.plusDays(20),
                now.plusDays(20),
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );
    }

    public static ExtractableResponse<Response> 예약_생성_성공() {
        ReservationRequest request = createReservationRequest();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(201);
        return response;
    }

    public static void 예약_취소_성공(long reservationId) {
        ExtractableResponse<Response> successResponse = 예약_생성_성공();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .param("confirmationCode", successResponse.jsonPath().getString("confirmationCode"))
                .when()
                .post("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(201);
    }

    public static ExtractableResponse<Response> getCreateReservationApiResponse(ReservationRequest request) {
        return RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();
    }
}
