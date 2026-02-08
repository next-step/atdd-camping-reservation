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

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Feature: 사이트 가용성 조회(단건)")
public class SiteAvailabilitySingleTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Campsite existingSite;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();

        // Given: a campsite exists
        existingSite = campsiteRepository.save(new Campsite("A-1", "Test site", 4));
    }

    @Test
    @DisplayName("Scenario: 정상 - 사이트 가용 여부 반환")
    void checkSiteAvailability() {
        // When: I GET "/api/sites/{siteNumber}/availability?date=..."
        // Then: response is 200 and body contains availability
        RestAssured.given().log().all()
                .queryParam("date", "2026-02-10")
                .when().get("/api/sites/" + existingSite.getSiteNumber() + "/availability")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("available", equalTo(true));
    }

    @Test
    @DisplayName("Scenario: 실패 - 과거 날짜 조회")
    void checkSiteAvailabilityForPastDate() {
        // When: I GET "/api/sites/{siteNumber}/availability?date=..." with a past date
        // Then: an error response is returned
        RestAssured.given().log().all()
                .queryParam("date", "2026-02-01")
                .when().get("/api/sites/" + existingSite.getSiteNumber() + "/availability")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
