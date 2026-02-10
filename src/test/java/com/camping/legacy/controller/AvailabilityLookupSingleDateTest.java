package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;

@DisplayName("Feature: 가용성 조회(단일 날짜)")
public class AvailabilityLookupSingleDateTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Campsite reservedSite;
    private Campsite availableSite;

    @BeforeEach
    void setUp() {
        super.setUp();

        // Given: a reservation exists for site "A-1"
        reservedSite = campsiteRepository.save(new Campsite("A-1", "Reserved Site", 4));
        availableSite = campsiteRepository.save(new Campsite("A-2", "Available Site", 4));
        Reservation reservation = new Reservation(
                "홍길동",
                LocalDate.parse("2030-02-10"),
                LocalDate.parse("2030-02-10"),
                reservedSite
        );
        reservation.setReservationDate(LocalDate.parse("2030-02-10"));
        reservationRepository.save(reservation);
    }

    @Test
    @DisplayName("Scenario: 정상 - 특정 날짜 가용 사이트 목록")
    void findAvailableSitesByDate() {
        // When: I GET "/api/sites/available?date=..."
        // Then: response is 200 and list does not include the reserved site
        RestAssured.given().log().all()
                .queryParam("date", "2030-02-10")
                .when().get("/api/sites/available")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("siteNumber", not(hasItem(reservedSite.getSiteNumber())))
                .body("siteNumber", hasItem(availableSite.getSiteNumber()));
    }

    @Test
    @DisplayName("Scenario: 실패 - date 파라미터 누락")
    void findAvailableSitesWithMissingDate() {
        // When: I GET "/api/sites/available" without a date
        // Then: response is 400
        RestAssured.given().log().all()
                .when().get("/api/sites/available")
                .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
