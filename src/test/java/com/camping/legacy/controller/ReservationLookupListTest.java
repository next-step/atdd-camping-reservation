package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@DisplayName("Feature: 예약 조회(목록)")
public class ReservationLookupListTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Reservation reservation1;

    @BeforeEach
    void setUp() {
        super.setUp();

        Campsite site1 = campsiteRepository.save(new Campsite("A-1", "Test site A", 4));
        Campsite site2 = campsiteRepository.save(new Campsite("B-1", "Test site B", 2));

        reservation1 = new Reservation("홍길동", LocalDate.parse("2030-02-10"), LocalDate.parse("2030-02-12"), site1);
        reservation1.setPhoneNumber("010-1234-5678");
        reservationRepository.save(reservation1);

        Reservation reservation2 = new Reservation("김철수", LocalDate.parse("2030-02-11"), LocalDate.parse("2030-02-13"), site2);
        reservationRepository.save(reservation2);
    }

    @Test
    @DisplayName("Scenario: 정상 - 날짜로 예약 조회")
    void findReservationsByDate() {
        Map<String, Object> queryParams = Map.of("date", "2030-02-11");
        get("/api/reservations", queryParams)
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("customerName", hasItems("홍길동", "김철수"));
    }

    @Test
    @DisplayName("Scenario: 정상 - 이름으로 예약 조회")
    void findReservationsByCustomerName() {
        Map<String, Object> queryParams = Map.of("customerName", "홍길동");
        get("/api/reservations", queryParams)
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].customerName", equalTo("홍길동"));
    }

    @Test
    @DisplayName("Scenario: 정상 - 이름+전화번호로 내 예약 조회")
    void findMyReservationsByNameAndPhone() {
        Map<String, Object> queryParams = Map.of(
                "name", "홍길동",
                "phone", "010-1234-5678"
        );
        get("/api/reservations/my", queryParams)
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].customerName", equalTo("홍길동"));
    }

    @Test
    @DisplayName("Scenario: 실패 - 잘못된 날짜 포맷")
    void findReservationsWithInvalidDateFormat() {
        Map<String, Object> queryParams = Map.of("date", "2030-02-30");
        get("/api/reservations", queryParams)
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
