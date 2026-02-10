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

import static org.hamcrest.Matchers.matchesRegex;

@DisplayName("Feature: 예약 생성")
public class ReservationCreationTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("Scenario: 정상 - 유효한 예약 생성")
    void createValidReservation() {
        // Given: campsite "A-1" exists
        campsiteRepository.save(new Campsite("A-1", "Test site", 4));

        // When: I POST "/api/reservations" with valid data
        Map<String, Object> requestBody = Map.of(
                "customerName", "홍길동",
                "phoneNumber", "010-1234-5678",
                "siteNumber", "A-1",
                "startDate", "2030-02-10",
                "endDate", "2030-02-12"
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("confirmationCode", matchesRegex("[a-zA-Z0-9]{6}"));
    }

    @Test
    @DisplayName("Scenario: 실패 - 기간 중복")
    void createReservationWithOverlappingDates() {
        // Given: campsite "A-1" has an existing reservation
        Campsite site = campsiteRepository.save(new Campsite("A-1", "Test site", 4));
        reservationRepository.save(new Reservation(
                "기존예약자",
                LocalDate.parse("2030-02-10"),
                LocalDate.parse("2030-02-12"),
                site
        ));

        // When: I POST "/api/reservations" with overlapping dates
        Map<String, Object> requestBody = Map.of(
                "customerName", "김철수",
                "phoneNumber", "010-2222-3333",
                "siteNumber", "A-1",
                "startDate", "2030-02-11",
                "endDate", "2030-02-13"
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 종료일이 시작일보다 이전")
    void createReservationWithEndDateBeforeStartDate() {
        // Given: campsite "A-1" exists
        campsiteRepository.save(new Campsite("A-1", "Test site", 4));

        // When: I POST "/api/reservations" with end date before start date
        Map<String, Object> requestBody = Map.of(
                "customerName", "이영희",
                "phoneNumber", "010-4444-5555",
                "siteNumber", "A-1",
                "startDate", "2030-02-12",
                "endDate", "2030-02-10"
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value());
    }
}
