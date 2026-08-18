package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;
import java.util.HashMap;
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

    @Test
    @DisplayName("전화번호를 입력하면 예약되고 확인 코드가 발급된다")
    void shouldCreateReservationWhenPhoneNumberIsProvided() {
        LocalDate startDate = LocalDate.now().plusDays(28);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "B-15", "010-9999-9999"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue())
                .body("confirmationCode", matchesPattern("[A-Z0-9]{6}"));
    }

    @Test
    @DisplayName("전화번호를 입력하지 않으면 예약이 거절된다")
    void shouldRejectReservationWhenPhoneNumberIsMissing() {
        LocalDate startDate = LocalDate.now().plusDays(27);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequestWithoutPhoneNumber(startDate, "B-14"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(400)
                .body("message", equalTo("전화번호를 입력해야 합니다."));
    }

    @Test
    @DisplayName("취소한 예약의 자리는 다시 예약할 수 있다")
    void shouldCreateReservationAfterPreviousReservationIsCancelled() {
        LocalDate startDate = LocalDate.now().plusDays(26);

        io.restassured.response.Response reservation = given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "B-5"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(201)
                .extract()
                .response();

        Number reservationId = reservation.path("id");
        String confirmationCode = reservation.path("confirmationCode");

        given()
                .queryParam("confirmationCode", confirmationCode)
        .when()
                .delete("/api/reservations/{id}", reservationId.longValue())
        .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "B-5"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue())
                .body("confirmationCode", matchesPattern("[A-Z0-9]{6}"));
    }

    @Test
    @DisplayName("예약이 된 자리에 예약하면 거절된다")
    void shouldRejectReservationWhenSiteIsAlreadyReserved() {
        LocalDate startDate = LocalDate.now().plusDays(25);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "B-6"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(startDate, "B-6"))
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(409)
                .body("message", equalTo("해당 기간에 이미 예약이 존재합니다."));
    }

    private Map<String, Object> reservationRequest(LocalDate startDate, String siteNumber) {
        return reservationRequest(startDate, siteNumber, "010-9999-9999");
    }

    private Map<String, Object> reservationRequest(
            LocalDate startDate, String siteNumber, String phoneNumber) {
        return Map.of(
                "customerName", "테스트고객",
                "startDate", startDate.toString(),
                "endDate", startDate.toString(),
                "siteNumber", siteNumber,
                "phoneNumber", phoneNumber
        );
    }

    private Map<String, Object> reservationRequestWithoutPhoneNumber(
            LocalDate startDate, String siteNumber) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerName", "테스트고객");
        request.put("startDate", startDate.toString());
        request.put("endDate", startDate.toString());
        request.put("siteNumber", siteNumber);
        return request;
    }
}
