package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

@DisplayName("Feature: 연박 예약")
public class MultiNightReservationTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        super.setUp();
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario: 정상 - 3박 예약")
    void createMultiNightReservation() {
        // Given: campsite "B-1" exists
        campsiteRepository.save(new Campsite("B-1", "Test site", 4));

        // When: I POST "/api/reservations" for 3 nights
        Map<String, Object> requestBody = Map.of(
                "customerName", "박민수",
                "phoneNumber", "010-6666-7777",
                "siteNumber", "B-1",
                "startDate", "2030-02-14",
                "endDate", "2030-02-16"
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 기간 중 일부 날짜에 예약 존재")
    void createMultiNightReservationWithPartialOverlap() {
        // Given: campsite "B-1" has an existing reservation
        Campsite site = campsiteRepository.save(new Campsite("B-1", "Test site", 4));
        reservationRepository.save(new Reservation(
                "기존예약자",
                LocalDate.parse("2030-02-15"),
                LocalDate.parse("2030-02-15"),
                site
        ));

        // When: I POST "/api/reservations" with partial overlap
        Map<String, Object> requestBody = Map.of(
                "customerName", "정수진",
                "phoneNumber", "010-8888-9999",
                "siteNumber", "B-1",
                "startDate", "2030-02-14",
                "endDate", "2030-02-16"
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value());
    }
}
