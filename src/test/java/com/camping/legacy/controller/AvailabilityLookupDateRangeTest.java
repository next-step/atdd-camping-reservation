package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Feature: 가용성 조회(기간)")
public class AvailabilityLookupDateRangeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario: 정상 - 기간 가용 사이트 + size 필터")
    void searchAvailableSitesWithSizeFilter() {
        // Given: campsites exist
        campsiteRepository.save(new Campsite("A-1", "대형 사이트", 8));
        campsiteRepository.save(new Campsite("B-1", "소형 사이트", 4));

        // When: I GET "/api/sites/search?..."
        // Then: response is 200 and list only contains large sites
        RestAssured.given().log().all()
                .queryParam("startDate", "2026-02-20")
                .queryParam("endDate", "2026-02-22")
                .queryParam("size", "대형")
                .when().get("/api/sites/search")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].description", containsString("대형"));
    }

    @Test
    @DisplayName("Scenario: 실패 - 종료일이 시작일보다 이전")
    void searchAvailableSitesWithEndDateBeforeStartDate() {
        // When: I GET "/api/sites/search?..." with invalid date range
        // Then: an error response is returned
        RestAssured.given().log().all()
                .queryParam("startDate", "2026-02-22")
                .queryParam("endDate", "2026-02-20")
                .when().get("/api/sites/search")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
