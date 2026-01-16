package com.camping.acceptance.steps;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class SiteSteps {

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> 가용_사이트_조회(LocalDate date) {
        return given()
                .when()
                    .get("/api/sites/available?date=" + date)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .jsonPath()
                    .getList("$");
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> 가용_사이트_검색(LocalDate startDate, LocalDate endDate) {
        return given()
                .when()
                    .get("/api/sites/search?startDate=" + startDate + "&endDate=" + endDate)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .jsonPath()
                    .getList("$");
    }
}
