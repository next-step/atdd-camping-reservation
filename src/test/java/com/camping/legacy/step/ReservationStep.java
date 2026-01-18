package com.camping.legacy.step;

import static io.restassured.RestAssured.given;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.fixture.ReservationFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class ReservationStep {
    private static final String RESERVATION_ENDPOINT = "/api/reservations";

    public static ExtractableResponse<Response> 예약을_요청한다(int startDayOffset, int endDayOffset, String name, String phoneNumber) {
        var request = 예약요청_생성(startDayOffset, endDayOffset, name, phoneNumber);
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

    private static ReservationRequest 예약요청_생성(int startDayOffset, int endDayOffset, String name, String phoneNumber) {
        var startDate = java.time.LocalDate.now().plusDays(startDayOffset);
        var endDate = java.time.LocalDate.now().plusDays(endDayOffset);
        return new ReservationRequest(name, startDate, endDate, ReservationFixture.SITE_A1, phoneNumber, 4, "12가3456", "잘 부탁드립니다.");
    }
}
