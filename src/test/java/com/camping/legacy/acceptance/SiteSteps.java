package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;

public class SiteSteps {

    public static final String SITES_URL = "/api/sites";

    public static ExtractableResponse<Response> searchAvailableSites(
            int startDaysFromNow,
            int endDaysFromNow
    ) {
        return given()
                    .queryParam("startDate", LocalDate.now().plusDays(startDaysFromNow).toString())
                    .queryParam("endDate", LocalDate.now().plusDays(endDaysFromNow).toString())
                .when()
                    .get(SITES_URL + "/search")
                .then()
                    .extract();
    }
}
