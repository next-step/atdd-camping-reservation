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

@DisplayName("Feature: 연박 예약")
public class MultiNightReservationTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("Scenario: 정상 - 3박 예약")
    void createMultiNightReservation() {
        // Given: campsite "B-1" exists
        campsiteRepository.save(new Campsite("B-1", "Test site", 4));

        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // When: I POST "/api/reservations" for 3 nights
        Map<String, Object> requestBody = Map.of(
                "customerName", "박민수",
                "phoneNumber", "010-6666-7777",
                "siteNumber", "B-1",
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 기간 중 일부 날짜에 예약 존재")
    void createMultiNightReservationWithPartialOverlap() {
        // Given: campsite "B-1" has an existing reservation
        Campsite site = campsiteRepository.save(new Campsite("B-1", "Test site", 4));

        LocalDate existingDate = LocalDate.now().plusDays(11);

        reservationRepository.save(new Reservation(
                "기존예약자",
                existingDate,
                existingDate,
                site
        ));

        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // When: I POST "/api/reservations" with partial overlap
        Map<String, Object> requestBody = Map.of(
                "customerName", "정수진",
                "phoneNumber", "010-8888-9999",
                "siteNumber", "B-1",
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );

        post("/api/reservations", requestBody)
                .statusCode(HttpStatus.CONFLICT.value());
    }
}