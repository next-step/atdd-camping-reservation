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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Feature: 예약 조회(단건)")
public class ReservationLookupSingleTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Reservation existingReservation;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();

        // Given: a reservation exists
        Campsite site = campsiteRepository.save(new Campsite("A-1", "Test site", 4));
        existingReservation = reservationRepository.save(new Reservation(
                "홍길동",
                LocalDate.parse("2026-02-10"),
                LocalDate.parse("2026-02-12"),
                site
        ));
    }

    @Test
    @DisplayName("Scenario: 정상 - 예약 ID로 조회")
    void findReservationById() {
        // When: I GET "/api/reservations/{id}"
        // Then: response status should be 200 and body contains the reservation
        RestAssured.given().log().all()
                .when().get("/api/reservations/" + existingReservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(existingReservation.getId().intValue()));
    }

    @Test
    @DisplayName("Scenario: 실패 - 존재하지 않는 예약 ID")
    void findReservationByNonExistentId() {
        // When: I GET "/api/reservations/{id}" with a non-existent id
        // Then: response status should be 404
        RestAssured.given().log().all()
                .when().get("/api/reservations/99999")
                .then().log().all()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
