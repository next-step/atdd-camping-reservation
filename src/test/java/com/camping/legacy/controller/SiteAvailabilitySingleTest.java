package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

@DisplayName("Feature: 사이트 가용성 조회(단건)")
public class SiteAvailabilitySingleTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Campsite existingSite;

    @BeforeEach
    void setUp() {
        super.setUp();

        // Given: a campsite exists
        existingSite = campsiteRepository.save(new Campsite("A-1", "Test site", 4));
    }

    @Test
    @DisplayName("Scenario: 정상 - 사이트 가용 여부 반환")
    void checkSiteAvailability() {
        // When: I GET "/api/sites/{siteNumber}/availability?date=..."
        // Then: response is 200 and body contains availability
        Map<String, Object> queryParams = Map.of("date", "2026-02-14");
        get("/api/sites/" + existingSite.getSiteNumber() + "/availability", queryParams)
                .statusCode(HttpStatus.OK.value())
                .body("available", equalTo(true));
    }

    @Test
    @DisplayName("Scenario: 정상 - 미래 날짜 조회")
    void checkSiteAvailabilityForFutureDate() {
        // When: I GET "/api/sites/{siteNumber}/availability?date=..." with a future date
        // Then: a success response is returned
        Map<String, Object> queryParams = Map.of("date", "2026-02-14");
        get("/api/sites/" + existingSite.getSiteNumber() + "/availability", queryParams)
                .statusCode(HttpStatus.OK.value())
                .body("available", equalTo(true));
    }

    @Test
    @DisplayName("Scenario: 실패 - 30일 이후 날짜 조회 불가")
    void checkSiteAvailabilityBeyond30Days() {
        String futureDate = LocalDate.now().plusDays(31).toString();
        Map<String, Object> queryParams = Map.of("date", futureDate);
        get("/api/sites/" + existingSite.getSiteNumber() + "/availability", queryParams)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
