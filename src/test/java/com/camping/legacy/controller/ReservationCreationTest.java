package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
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
        String existingSiteNumber = "A-1";
        campsiteRepository.save(new Campsite(existingSiteNumber, "Test site", 4));

        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(7);

        // When: I POST "/api/reservations" with valid data
        Map<String, Object> requestBody = Map.of(
                "customerName", "홍길동",
                "phoneNumber", "010-1234-5678",
                "siteNumber", existingSiteNumber,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CREATED.value())
                .body("confirmationCode", matchesRegex("[a-zA-Z0-9]{6}"));
    }

    @Test
    @DisplayName("Scenario: 실패 - 기간 중복")
    void createReservationWithOverlappingDates() {
        // Given: campsite "A-1" has an existing reservation
        String existingSiteNumber = "A-1";
        Campsite site = campsiteRepository.save(new Campsite(existingSiteNumber, "Test site", 4));

        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(7);

        reservationRepository.save(new Reservation(
                "기존예약자",
                startDate,
                endDate,
                site
        ));

        // When: I POST "/api/reservations" with overlapping dates
        Map<String, Object> requestBody = Map.of(
                "customerName", "김철수",
                "phoneNumber", "010-2222-3333",
                "siteNumber", existingSiteNumber,
                "startDate", startDate.plusDays(1).toString(),
                "endDate", endDate.plusDays(1).toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 종료일이 시작일보다 이전")
    void createReservationWithEndDateBeforeStartDate() {
        // Given: campsite "A-1" exists
        String existingSiteNumber = "A-1";
        campsiteRepository.save(new Campsite(existingSiteNumber, "Test site", 4));

        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = LocalDate.now().plusDays(5);

        // When: I POST "/api/reservations" with end date before start date
        Map<String, Object> requestBody = Map.of(
                "customerName", "이영희",
                "phoneNumber", "010-4444-5555",
                "siteNumber", existingSiteNumber,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 30일 이후 예약 불가")
    void createReservationBeyond30Days() {
        // Given: campsite "A-1" exists
        String existingSiteNumber = "A-1";
        campsiteRepository.save(new Campsite(existingSiteNumber, "Test site", 4));

        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = LocalDate.now().plusDays(33);

        // When: I POST "/api/reservations" with start date beyond 30 days
        Map<String, Object> requestBody = Map.of(
                "customerName", "박영수",
                "phoneNumber", "010-5555-6666",
                "siteNumber", existingSiteNumber,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Scenario: 정상 - 30일째 되는 날 예약 가능")
    void createReservationExactly30Days() {
        // Given: campsite "A-1" exists
        String existingSiteNumber = "A-1";
        campsiteRepository.save(new Campsite(existingSiteNumber, "Test site", 4));

        LocalDate startDate = LocalDate.now().plusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(30);

        // When: I POST "/api/reservations" with start date exactly 30 days from now
        Map<String, Object> requestBody = Map.of(
                "customerName", "최경민",
                "phoneNumber", "010-7777-8888",
                "siteNumber", existingSiteNumber,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CREATED.value())
                .body("confirmationCode", matchesRegex("[a-zA-Z0-9]{6}"));
    }
}