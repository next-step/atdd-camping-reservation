package com.camping.legacy;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
 * T-3: 동일 사이트, 동일 기간에 중복 예약은 불가하다 / 취소된 예약은 중복 체크에서 제외된다.
 *
 * 인수 조건: docs/acceptance-criteria.md
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationDuplicateBookingAcceptanceTest {

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
    @DisplayName("T-3: 동일 사이트, 동일 기간에 중복 예약은 불가하다")
    class T3_동일_사이트_동일_기간에_중복_예약은_불가하다 {

        @Nested
        @DisplayName("예약 생성")
        class 예약_생성 {

            @Test
            @DisplayName("동일 사이트에 완전히 같은 기간으로 예약하면 거부된다")
            void 동일_사이트에_완전히_같은_기간으로_예약하면_거부된다() {
                String startDate = LocalDate.now().plusDays(3).toString();
                String endDate = LocalDate.now().plusDays(5).toString();

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-10",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터",
                          "phoneNumber": "010-1111-2222"
                        }
                        """.formatted(startDate, endDate))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201);

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-10",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터2",
                          "phoneNumber": "010-1111-3333"
                        }
                        """.formatted(startDate, endDate))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(409);
            }

            @Test
            @DisplayName("동일 사이트에 하루라도 겹치는 기간으로 예약하면 거부된다")
            void 동일_사이트에_하루라도_겹치는_기간으로_예약하면_거부된다() {
                String firstStart = LocalDate.now().plusDays(3).toString();
                String firstEnd = LocalDate.now().plusDays(5).toString();
                String secondStart = LocalDate.now().plusDays(5).toString();
                String secondEnd = LocalDate.now().plusDays(7).toString();

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-13",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터",
                          "phoneNumber": "010-1111-2222"
                        }
                        """.formatted(firstStart, firstEnd))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201);

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-13",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터3",
                          "phoneNumber": "010-1111-4444"
                        }
                        """.formatted(secondStart, secondEnd))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(409);
            }

            @Test
            @DisplayName("동일 사이트라도 기간이 겹치지 않으면 예약이 완료된다")
            void 동일_사이트라도_기간이_겹치지_않으면_예약이_완료된다() {
                String firstStart = LocalDate.now().plusDays(3).toString();
                String firstEnd = LocalDate.now().plusDays(5).toString();
                String secondStart = LocalDate.now().plusDays(6).toString();
                String secondEnd = LocalDate.now().plusDays(7).toString();

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-14",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터",
                          "phoneNumber": "010-1111-2222"
                        }
                        """.formatted(firstStart, firstEnd))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201);

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-14",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터4",
                          "phoneNumber": "010-1111-5555"
                        }
                        """.formatted(secondStart, secondEnd))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201);
            }
        }
    }

    @Nested
    @DisplayName("T-3: 취소된 예약은 중복 체크에서 제외된다")
    class T3_취소된_예약은_중복_체크에서_제외된다 {

        @Nested
        @DisplayName("예약 생성")
        class 예약_생성 {

            @Test
            @DisplayName("취소된(CANCELLED) 예약의 자리에 같은 기간으로 재예약하면 예약이 완료된다")
            void 취소된_CANCELLED_예약의_자리에_같은_기간으로_재예약하면_예약이_완료된다() {
                String startDate = LocalDate.now().plusDays(3).toString();
                String endDate = LocalDate.now().plusDays(5).toString();

                Response created = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-15",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터",
                          "phoneNumber": "010-1111-2222"
                        }
                        """.formatted(startDate, endDate))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201)
                    .extract().response();

                long id = ((Number) created.path("id")).longValue();
                String confirmationCode = created.path("confirmationCode");

                given()
                    .queryParam("confirmationCode", confirmationCode)
                .when()
                    .delete("/api/reservations/" + id)
                .then()
                    .statusCode(200);

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-15",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터5",
                          "phoneNumber": "010-1111-6666"
                        }
                        """.formatted(startDate, endDate))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201);
            }

            @Test
            @DisplayName("취소된(CANCELLED_SAME_DAY) 예약의 자리에 같은 기간으로 재예약하면 예약이 완료된다")
            void 취소된_CANCELLED_SAME_DAY_예약의_자리에_같은_기간으로_재예약하면_예약이_완료된다() {
                String today = LocalDate.now().toString();

                Response created = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-16",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터",
                          "phoneNumber": "010-1111-2222"
                        }
                        """.formatted(today, today))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201)
                    .extract().response();

                long id = ((Number) created.path("id")).longValue();
                String confirmationCode = created.path("confirmationCode");

                given()
                    .queryParam("confirmationCode", confirmationCode)
                .when()
                    .delete("/api/reservations/" + id)
                .then()
                    .statusCode(200);

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                          "siteNumber": "A-16",
                          "startDate": "%s",
                          "endDate": "%s",
                          "customerName": "테스터7",
                          "phoneNumber": "010-1111-8888"
                        }
                        """.formatted(today, today))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201);
            }
        }
    }
}
