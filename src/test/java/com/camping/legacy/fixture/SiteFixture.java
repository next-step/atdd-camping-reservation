package com.camping.legacy.fixture;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

public class SiteFixture {

    public static ExtractableResponse<Response> getAllSites() {
        return RestAssured.given()
                .when()
                .get("/api/sites")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> getSiteById(Long siteId) {
        return RestAssured.given()
                .when()
                .get("/api/sites/{siteId}", siteId)
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> getAvailableSites(LocalDate date) {
        return RestAssured.given()
                .param("date", date.toString())
                .when()
                .get("/api/sites/available")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> searchAvailableSites(LocalDate startDate, LocalDate endDate) {
        return RestAssured.given()
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .when()
                .get("/api/sites/search")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> searchAvailableSitesWithSize(LocalDate startDate, LocalDate endDate, String size) {
        return RestAssured.given()
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("size", size)
                .when()
                .get("/api/sites/search")
                .then()
                .extract();
    }

    public static ExtractableResponse<Response> checkSiteAvailability(String siteNumber, LocalDate date) {
        return RestAssured.given()
                .param("date", date.toString())
                .when()
                .get("/api/sites/{siteNumber}/availability", siteNumber)
                .then()
                .extract();
    }
}
