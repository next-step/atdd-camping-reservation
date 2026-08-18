package com.camping.legacy.acceptance;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReservationAcceptanceTest.FixedClockConfiguration.class)
class ReservationAcceptanceTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 18);

    @LocalServerPort
    private int port;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
    }

    @DisplayName("예약 시작일이 오늘로부터 30일 후이면 예약할 수 있다")
    @Test
    void createReservationOnLastAvailableStartDate() {
        LocalDate startDate = TODAY.plusDays(30);
        LocalDate endDate = startDate.plusDays(1);

        RestAssured.given()
                .contentType(JSON)
                .body(reservationRequest("B-14", startDate, endDate))
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("startDate", equalTo(startDate.toString()))
                .body("endDate", equalTo(endDate.toString()))
                .body("siteNumber", equalTo("B-14"));
    }

    @DisplayName("예약 시작일이 오늘로부터 31일 후이면 예약할 수 없다")
    @Test
    void rejectReservationAfterAvailableStartDate() {
        LocalDate startDate = TODAY.plusDays(31);
        LocalDate endDate = startDate.plusDays(1);

        Response createResponse = RestAssured.given()
                .contentType(JSON)
                .body(reservationRequest("B-15", startDate, endDate))
                .when()
                .post("/api/reservations");

        List<String> reservedSiteNumbers = RestAssured.given()
                .queryParam("date", startDate.toString())
                .when()
                .get("/api/reservations")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("siteNumber", String.class);

        assertAll(
                () -> assertEquals(409, createResponse.statusCode()),
                () -> assertEquals(
                        "예약 시작일은 오늘로부터 30일 이내여야 합니다.",
                        createResponse.jsonPath().getString("message")
                ),
                () -> assertFalse(reservedSiteNumbers.contains("B-15"))
        );
    }

    private Map<String, Object> reservationRequest(
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Map.of(
                "customerName", "홍길동",
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "siteNumber", siteNumber,
                "phoneNumber", "010-1234-5678"
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY.atStartOfDay(ZONE_ID).toInstant(), ZONE_ID);
        }
    }
}
