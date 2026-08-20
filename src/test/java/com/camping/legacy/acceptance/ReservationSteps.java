package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReservationSteps {

    public static Response create(String customerName, String siteNumber, LocalDate startDate) {
        return post(requestBody(customerName, siteNumber, startDate));
    }

    public static Response createWithPhoneNumber(String customerName, String siteNumber, LocalDate startDate,
                                                 String phoneNumber) {
        Map<String, Object> body = requestBody(customerName, siteNumber, startDate);
        body.put("phoneNumber", phoneNumber);
        return post(body);
    }

    public static Response createWithoutPhoneNumber(String customerName, String siteNumber, LocalDate startDate) {
        Map<String, Object> body = requestBody(customerName, siteNumber, startDate);
        body.remove("phoneNumber");
        return post(body);
    }

    private static Response post(Map<String, Object> body) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/reservations");
    }

    public static Response updateStartDate(JsonPath reserved, LocalDate startDate) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", reserved.getString("confirmationCode"))
                .body(requestBody(reserved.getString("customerName"), reserved.getString("siteNumber"), startDate))
                .when()
                .put("/api/reservations/{id}", reserved.getLong("id"));
    }

    public static Response findByCustomerName(String customerName) {
        return RestAssured.given()
                .queryParam("customerName", customerName)
                .when()
                .get("/api/reservations");
    }

    public static Response findById(long id) {
        return RestAssured.given()
                .when()
                .get("/api/reservations/{id}", id);
    }

    private static Map<String, Object> requestBody(String customerName, String siteNumber, LocalDate startDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerName", customerName);
        body.put("startDate", startDate.toString());
        body.put("endDate", startDate.toString());
        body.put("siteNumber", siteNumber);
        body.put("phoneNumber", "010-1111-2222");
        body.put("numberOfPeople", 2);
        return body;
    }
}
