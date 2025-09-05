package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class AcceptanceTestFixTure {
    private static final LocalDate now = LocalDate.now();
    private static final LocalDate startDate = now.plusDays(20);

    public static ReservationRequest createReservationRequest() {
        return new ReservationRequest(
                "김영희",
                startDate,
                startDate,
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createSameReservationRequest() {
        return new ReservationRequest(
                "박철수",
                startDate,
                startDate,
                "A-1",
                "010-9876-5432",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createCancelledReservationRequest() {
        예약_취소_성공();
        return new ReservationRequest(
                "김영희",
                startDate,
                startDate,
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createCancelledSameReservationRequest() {
        예약_취소_성공();
        return new ReservationRequest(
                "박철수",
                startDate,
                startDate,
                "A-1",
                "010-9876-5432",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createWrongReservationRequest() {
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
        return new ReservationRequest(
                "김영희",
                startDate,
                startDate,
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

    public static ExtractableResponse<Response> 예약_취소_성공() {
        ExtractableResponse<Response> successResponse = 예약_생성_성공();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .param("confirmationCode", successResponse.jsonPath().getString("confirmationCode"))
                .when()
                .delete("/api/reservations/" + successResponse.jsonPath().getLong("id"))
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    public static ReservationRequest createConsecutiveReservationRequest() {
        예약_취소_성공();
        return new ReservationRequest(
                "김연박",
                startDate,
                startDate.plusDays(2),
                "B-2",
                "010-1111-2222",
                4,
                null,
                null
        );
    }

    public static ReservationRequest createConsecutiveWithCancelledReservationRequest() {
        예약_취소_성공();
        return new ReservationRequest(
                "박연박",
                startDate,
                startDate.plusDays(2),
                "C-3",
                "010-3333-4444",
                3,
                null,
                null
        );
    }

    public static ReservationRequest createBlockedConsecutiveReservationRequest() {
        return new ReservationRequest(
                "이연박",
                now.plusDays(5),
                now.plusDays(7),
                "A-1",
                "010-5555-6666",
                2,
                null,
                null
        );
    }

    public static ReservationRequest createExistingReservationInConsecutivePeriod() {
        return new ReservationRequest(
                "최기존",
                now.plusDays(6),
                now.plusDays(6),
                "A-1",
                "010-7777-8888",
                2,
                null,
                null
        );
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
