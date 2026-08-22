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
import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-1: 요구사항 2, 3
 * - 과거 날짜로 예약할 수 없고, 종료일이 시작일보다 이전일 수 없다.
 * - 예약 완료 시 6자리 영숫자 확인 코드가 생성된다.
 *
 * 인수 조건: docs/acceptance-criteria.md
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationDateValidationAcceptanceTest {

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
    @DisplayName("T-1: 과거 날짜로 예약할 수 없고, 종료일이 시작일보다 이전일 수 없다")
    class T1_과거_날짜로_예약할_수_없고_종료일이_시작일보다_이전일_수_없다 {

        @Nested
        @DisplayName("예약 생성")
        class 예약_생성 {

            @Test
            @DisplayName("과거 날짜로 예약하면 거부된다")
            void 과거_날짜로_예약하면_거부된다() {
                String yesterday = LocalDate.now().minusDays(1).toString();

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "siteNumber": "A-5",
                              "startDate": "%s",
                              "endDate": "%s",
                              "customerName": "테스터",
                              "phoneNumber": "010-1111-2222"
                            }
                            """.formatted(yesterday, yesterday))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(409);
            }

            @Test
            @DisplayName("종료일이 시작일보다 이전이면 거부된다")
            void 종료일이_시작일보다_이전이면_거부된다() {
                String startDate = LocalDate.now().plusDays(2).toString();
                String endDate = LocalDate.now().plusDays(1).toString();

                given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "siteNumber": "A-7",
                              "startDate": "%s",
                              "endDate": "%s",
                              "customerName": "테스터",
                              "phoneNumber": "010-1111-2222"
                            }
                            """.formatted(startDate, endDate))
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
            @DisplayName("시작일만 과거로 수정해도 거부된다")
            void 시작일만_과거로_수정해도_거부된다() {
                String tomorrow = LocalDate.now().plusDays(1).toString();

                var created = given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "siteNumber": "A-5",
                              "startDate": "%s",
                              "endDate": "%s",
                              "customerName": "테스터",
                              "phoneNumber": "010-1111-2222"
                            }
                            """.formatted(tomorrow, tomorrow))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201)
                    .extract().response();

                long id = ((Number) created.path("id")).longValue();
                String confirmationCode = created.path("confirmationCode");
                String yesterday = LocalDate.now().minusDays(1).toString();

                given()
                    .contentType(ContentType.JSON)
                    .queryParam("confirmationCode", confirmationCode)
                    .body("""
                            {
                              "startDate": "%s"
                            }
                            """.formatted(yesterday))
                .when()
                    .put("/api/reservations/" + id)
                .then()
                    .statusCode(400);
            }
        }
    }

    @Nested
    @DisplayName("T-1: 예약 완료 시 6자리 영숫자 확인 코드가 생성된다")
    class T1_예약_완료_시_6자리_영숫자_확인_코드가_생성된다 {

        @Nested
        @DisplayName("예약 생성")
        class 예약_생성 {

            @Test
            @DisplayName("예약이 완료되면 6자리 영숫자 확인 코드가 생성된다")
            void 예약이_완료되면_6자리_영숫자_확인_코드가_생성된다() {
                String tomorrow = LocalDate.now().plusDays(1).toString();

                String confirmationCode = given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "siteNumber": "A-9",
                              "startDate": "%s",
                              "endDate": "%s",
                              "customerName": "테스터",
                              "phoneNumber": "010-1111-2222"
                            }
                            """.formatted(tomorrow, tomorrow))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(201)
                    .extract().path("confirmationCode");

                assertThat(confirmationCode).matches("[A-Z0-9]{6}");
            }
        }
    }
}
