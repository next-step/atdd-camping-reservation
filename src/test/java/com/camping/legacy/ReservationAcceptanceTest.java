package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("시작일이 29일 뒤면 예약되고 확인 코드가 발급된다")
    void shouldCreateReservationWhenStartDateIsWithin29Days() {
        LocalDate startDate = LocalDate.now().plusDays(29);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "A-20"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue())
                .body("confirmationCode", matchesPattern("[A-Z0-9]{6}"));
    }

    @Test
    @DisplayName("시작일이 31일 뒤면 예약이 거절된다")
    void shouldRejectReservationWhenStartDateExceeds30Days() {
        LocalDate startDate = LocalDate.now().plusDays(31);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "B-2"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(409)
                .body("message", equalTo("예약 시작일은 오늘로부터 30일 이내여야 합니다."));
    }

    private Map<String, Object> reservationRequest(LocalDate startDate, String siteNumber) {
        return Map.of(
                "customerName", "테스트고객",
                "startDate", startDate.toString(),
                "endDate", startDate.toString(),
                "siteNumber", siteNumber,
                "phoneNumber", "010-9999-9999"
        );
    }
}
