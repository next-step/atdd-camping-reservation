package com.camping.acceptance.reservation;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReservationSteps {

    public static ExtractableResponse<Response> 예약_생성_요청(String siteNumber, String customerName,
                                                          String phone, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("customerName", customerName);
        request.put("phoneNumber", phone);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_생성_요청(Map<String, Object> request) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_수정_요청(Long reservationId, String confirmationCode,
                                                          Map<String, Object> request) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("confirmationCode", confirmationCode)
                .body(request)
                .when().put("/api/reservations/{id}", reservationId)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_취소_요청(Long reservationId, String confirmationCode) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/{id}", reservationId)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_조회_요청(Long reservationId) {
        return RestAssured
                .given().log().all()
                .when().get("/api/reservations/{id}", reservationId)
                .then().log().all()
                .extract();
    }
}
