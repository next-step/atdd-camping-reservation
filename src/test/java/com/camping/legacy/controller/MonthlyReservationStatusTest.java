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

import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Feature: 월별 예약 현황")
public class MonthlyReservationStatusTest {

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

        existingSite = campsiteRepository.save(new Campsite("A-1", "Test Site", 4));
        Reservation reservation = new Reservation(
                "홍길동",
                LocalDate.parse("2026-02-15"),
                LocalDate.parse("2026-02-15"),
                existingSite
        );
        reservation.setReservationDate(LocalDate.parse("2026-02-15"));
        reservationRepository.save(reservation);
    }

    @Test
    @DisplayName("Scenario: 정상 - 월별 캘린더 조회")
    void getMonthlyCalendar() {
        RestAssured.given().log().all()
                .queryParam("year", 2026)
                .queryParam("month", 2)
                .queryParam("siteId", existingSite.getId())
                .when().get("/api/reservations/calendar")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("days", hasSize(28)) // 2026 is not a leap year
                .body("days[14].date", equalTo("2026-02-15"))
                .body("days[14].available", equalTo(false));
    }

    @Test
    @DisplayName("Scenario: 실패 - 존재하지 않는 사이트 ID")
    void getMonthlyCalendarForNonExistentSite() {
        RestAssured.given().log().all()
                .queryParam("year", 2026)
                .queryParam("month", 2)
                .queryParam("siteId", 99999)
                .when().get("/api/reservations/calendar")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()); // Assuming 500 for now, could be 404
    }
}
