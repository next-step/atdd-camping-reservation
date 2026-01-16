package com.camping.legacy.acceptance.steps;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class ReservationSteps {

    public static ExtractableResponse<Response> 예약_요청(String siteNumber, String customerName,
            String phoneNumber, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("numberOfPeople", 4);

        return given()
                    .contentType(JSON)
                    .body(request)
                .when()
                    .post("/api/reservations")
                .then()
                    .extract();
    }

    public static ExtractableResponse<Response> 예약_취소(Long reservationId, String confirmationCode) {
        return given()
                .when()
                    .delete("/api/reservations/" + reservationId + "?confirmationCode=" + confirmationCode)
                .then()
                    .extract();
    }

    public static List<?> 예약_목록_조회(LocalDate date) {
        return given()
                .when()
                    .get("/api/reservations?date=" + date.toString())
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .jsonPath()
                    .getList("$");
    }
}
