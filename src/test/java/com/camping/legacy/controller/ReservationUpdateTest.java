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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Feature: 예약 수정")
public class ReservationUpdateTest {

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
    @DisplayName("Scenario: 정상 - 확인 코드로 예약 수정")
    void updateReservationWithValidCode() {
        Map<String, Object> requestBody = Map.of(
                "customerName", "홍길동",
                "startDate", "2026-02-20",
                "endDate", "2026-02-22"
        );

        RestAssured.given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().put("/api/reservations/" + existingReservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("startDate", equalTo("2026-02-20"))
                .body("endDate", equalTo("2026-02-22"));
    }

    @Test
    @DisplayName("Scenario: 실패 - 확인 코드 불일치")
    void updateReservationWithInvalidCode() {
        Map<String, Object> requestBody = Map.of("customerName", "홍길동");

        RestAssured.given().log().all()
                .queryParam("confirmationCode", "WRONG")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().put("/api/reservations/" + existingReservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 종료일이 시작일보다 이전")
    void updateReservationWithEndDateBeforeStartDate() {
        Map<String, Object> requestBody = Map.of(
                "startDate", "2026-02-12",
                "endDate", "2026-02-10"
        );

        RestAssured.given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().put("/api/reservations/" + existingReservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
