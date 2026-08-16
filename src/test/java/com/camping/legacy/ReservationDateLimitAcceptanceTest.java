package com.camping.legacy;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;

/**
 * T-1: 오늘로부터 30일 넘게 남은 날짜 예약 거부
 *
 * 인수 조건: docs/acceptance-criteria.md
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationDateLimitAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    ReservationRepository reservationRepository;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        log.info("\n\n======== setUp completed — [{}] ========\n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("예약 생성")
    class 예약_생성 {

        @Test
        @DisplayName("오늘로부터 30일 이내 날짜로 예약하면 예약이 완료된다")
        void 오늘로부터_30일_이내_날짜로_예약하면_예약이_완료된다() {
            String startDate = LocalDate.now().plusDays(30).toString();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-1",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(startDate, startDate))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("30일 초과 첫 경계 날짜로 예약하면 거부된다")
        void 삼십일_초과_첫_경계_날짜로_예약하면_거부된다() {
            String startDate = LocalDate.now().plusDays(31).toString();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-1",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(startDate, startDate))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409);
        }

        @Test
        @DisplayName("30일을 크게 초과한 날짜로 예약해도 거부된다")
        void 삼십일을_크게_초과한_날짜로_예약해도_거부된다() {
            String startDate = LocalDate.now().plusDays(60).toString();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-1",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(startDate, startDate))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409);
        }
    }

    @Nested
    @DisplayName("예약 수정")
    class 예약_수정 {

        @Test
        @DisplayName("30일을 초과한 날짜로 예약 일정을 변경하면 거부된다")
        void 삼십일을_초과한_날짜로_예약_일정을_변경하면_거부된다() {
            String validDate = LocalDate.now().plusDays(1).toString();

            var created = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-1",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(validDate, validDate))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .extract().response();

            long id = ((Number) created.path("id")).longValue();
            String confirmationCode = created.path("confirmationCode");
            String overDate = LocalDate.now().plusDays(31).toString();

            given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body("""
                    {
                      "startDate": "%s",
                      "endDate": "%s"
                    }
                    """.formatted(overDate, overDate))
            .when()
                .put("/api/reservations/" + id)
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("시작일만 30일 초과로 변경해도 거부된다")
        void 시작일만_삼십일_초과로_변경해도_거부된다() {
            String validDate = LocalDate.now().plusDays(1).toString();

            var created = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-1",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(validDate, validDate))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .extract().response();

            long id = ((Number) created.path("id")).longValue();
            String confirmationCode = created.path("confirmationCode");
            String overDate = LocalDate.now().plusDays(31).toString();

            given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body("""
                    {
                      "startDate": "%s"
                    }
                    """.formatted(overDate))
            .when()
                .put("/api/reservations/" + id)
            .then()
                .statusCode(400);
        }
    }
}
