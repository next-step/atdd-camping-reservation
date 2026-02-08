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
@DisplayName("Feature: 예약 취소")
public class ReservationCancellationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Reservation existingReservation;
    private final String confirmationCode = "ABC123";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();

        Campsite site = campsiteRepository.save(new Campsite("A-1", "Test site", 4));
        existingReservation = new Reservation("홍길동", LocalDate.parse("2026-02-10"), LocalDate.parse("2026-02-12"), site);
        existingReservation.setConfirmationCode(confirmationCode);
        reservationRepository.save(existingReservation);
    }

    @Test
    @DisplayName("Scenario: 정상 - 확인 코드로 취소")
    void cancelReservationWithValidCode() {
        RestAssured.given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + existingReservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("message", equalTo("예약이 취소되었습니다."));
    }

    @Test
    @DisplayName("Scenario: 실패 - 확인 코드 불일치")
    void cancelReservationWithInvalidCode() {
        RestAssured.given().log().all()
                .queryParam("confirmationCode", "WRONG")
                .when().delete("/api/reservations/" + existingReservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
