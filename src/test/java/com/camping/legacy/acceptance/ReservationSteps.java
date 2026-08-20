package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ReservationSteps {

    public static Response create(String customerName, String siteNumber, LocalDate startDate) {
        return create(customerName, siteNumber, startDate, startDate);
    }

    public static Response create(String customerName, String siteNumber, LocalDate startDate, LocalDate endDate) {
        return post(requestBody(customerName, siteNumber, startDate, endDate));
    }

    public static JsonPath create(String siteNumber, LocalDate startDate, LocalDate endDate) {
        JsonPath reserved = post(requestBody("기준" + siteNumber, siteNumber, startDate, endDate))
            .then()
            .statusCode(201)
            .extract()
            .jsonPath();

        assertConfirmedCount(startDate, siteNumber, 1);

        return reserved;
    }

    public static Response createWithPhoneNumber(String customerName, String siteNumber, LocalDate startDate,
                                                 String phoneNumber) {
        Map<String, Object> body = requestBody(customerName, siteNumber, startDate, startDate);
        body.put("phoneNumber", phoneNumber);
        return post(body);
    }

    public static Response createWithoutPhoneNumber(String customerName, String siteNumber, LocalDate startDate) {
        Map<String, Object> body = requestBody(customerName, siteNumber, startDate, startDate);
        body.remove("phoneNumber");
        return post(body);
    }

    public static JsonPath createCancelled(String siteNumber, LocalDate startDate, LocalDate endDate) {
        JsonPath reserved = create(siteNumber, startDate, endDate);

        delete(reserved)
                .then()
                .statusCode(200)
                .body("message", equalTo("예약이 취소되었습니다."));

        assertConfirmedCount(startDate, siteNumber, 0);

        return reserved;
    }

    public static Response updateStartDate(JsonPath reserved, LocalDate startDate) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", reserved.getString("confirmationCode"))
                .body(requestBody(reserved.getString("customerName"), reserved.getString("siteNumber"),
                        startDate, startDate))
                .when()
                .put("/api/reservations/{id}", reserved.getLong("id"));
    }

    public static Response findByCustomerName(String customerName) {
        return RestAssured.given()
                .queryParam("customerName", customerName)
                .when()
                .get("/api/reservations");
    }

    public static Response findByDate(LocalDate date) {
        return RestAssured.given()
                .queryParam("date", date.toString())
                .when()
                .get("/api/reservations");
    }

    /**
     * 그 이름으로 남은 예약이 없는지 본다. 거절해 놓고 저장하는 경우를 잡는다.
     */
    public static void assertNotReserved(String customerName) {
        findByCustomerName(customerName)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    public static Response findById(long id) {
        return RestAssured.given()
                .when()
                .get("/api/reservations/{id}", id);
    }

    public static void assertConfirmedCount(LocalDate date, String siteNumber, int expected) {
        findByDate(date)
            .then()
            .statusCode(200)
            .body("findAll { it.siteNumber == '%s' && it.status == 'CONFIRMED' }".formatted(siteNumber),
                hasSize(expected));
    }

    private static Map<String, Object> requestBody(String customerName, String siteNumber,
                                                   LocalDate startDate, LocalDate endDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerName", customerName);
        body.put("startDate", startDate.toString());
        body.put("endDate", endDate.toString());
        body.put("siteNumber", siteNumber);
        body.put("phoneNumber", "010-1111-2222");
        body.put("numberOfPeople", 2);
        return body;
    }

    private static Response post(Map<String, Object> body) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/reservations");
    }

    private static Response delete(JsonPath reserved) {
        return RestAssured.given()
            .queryParam("confirmationCode", reserved.getString("confirmationCode"))
            .when()
            .delete("/api/reservations/{id}", reserved.getLong("id"));
    }
}
