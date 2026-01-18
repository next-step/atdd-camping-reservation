package com.camping.legacy.step;

import static io.restassured.RestAssured.given;
import static java.time.temporal.TemporalAdjusters.*;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class ReservationStep {
    private static final String RESERVATION_ENDPOINT = "/api/reservations";

    public static ExtractableResponse<Response> 예약을_요청한다(int startDayOffset, int endDayOffset, String customerName, String siteNumber, String phoneNumber) {
        var request = 예약요청_생성(startDayOffset, endDayOffset, customerName, siteNumber, phoneNumber);
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

    public static ExtractableResponse<Response> 성수기_주말에_예약을_요청한다(int peakMonth, String name, String siteNumber, String phoneNumber) {
        var saturday = 성수기_첫_토요일_계산(peakMonth);
        var sunday = saturday.plusDays(1);

        var request = new ReservationRequest(
                name, saturday, sunday, siteNumber, phoneNumber, 4, "12가3456", "성수기 주말 예약입니다."
        );

        return given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .extract();
    }

    private static ReservationRequest 예약요청_생성(
        int startDayOffset, int endDayOffset, String name, String siteNumber, String phoneNumber) {
        var startDate = LocalDate.now().plusDays(startDayOffset);
        var endDate = LocalDate.now().plusDays(endDayOffset);
        return new ReservationRequest(
            name, startDate, endDate, siteNumber, phoneNumber, 4, "12가3456", "잘 부탁드립니다.");
    }

    private static LocalDate 성수기_첫_토요일_계산(int peakMonth) {
        var today = LocalDate.now();
        var targetDate = LocalDate.of(today.getYear(), peakMonth, 1);

        if (today.isAfter(targetDate.with(lastDayOfMonth()))) {
            targetDate = targetDate.plusYears(1);
        }

        return targetDate.with(nextOrSame(DayOfWeek.SATURDAY));
    }
}
