package com.camping.legacy.acceptance;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    @DisplayName("하이픈이 있는 010 전화번호로 예약할 수 있다")
    @Test
    void createReservationWithHyphenatedPhoneNumber() {
        LocalDate reservationDate = TODAY.plusDays(1);

        RestAssured.given()
                .contentType(JSON)
                .body(reservationRequest("B-1", reservationDate, reservationDate, "010-1234-5678"))
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("siteNumber", equalTo("B-1"))
                .body("phoneNumber", equalTo("010-1234-5678"));
    }

    @DisplayName("하이픈이 없는 010 전화번호로 예약할 수 있다")
    @Test
    void createReservationWithDigitsOnlyPhoneNumber() {
        LocalDate reservationDate = TODAY.plusDays(1);

        RestAssured.given()
                .contentType(JSON)
                .body(reservationRequest("B-2", reservationDate, reservationDate, "01012345678"))
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("siteNumber", equalTo("B-2"))
                .body("phoneNumber", equalTo("01012345678"));
    }

    @DisplayName("전화번호를 누락하면 예약할 수 없다")
    @Test
    void rejectReservationWithoutPhoneNumber() {
        LocalDate reservationDate = TODAY.plusDays(1);
        Map<String, Object> request = reservationRequest(
                "B-3",
                reservationDate,
                reservationDate,
                "010-1234-5678"
        );
        request.remove("phoneNumber");

        assertReservationRejected(
                request,
                "B-3",
                reservationDate,
                "전화번호를 입력해주세요."
        );
    }

    @DisplayName("전화번호가 null이면 예약할 수 없다")
    @Test
    void rejectReservationWithNullPhoneNumber() {
        LocalDate reservationDate = TODAY.plusDays(1);

        assertReservationRejected(
                reservationRequest("B-4", reservationDate, reservationDate, null),
                "B-4",
                reservationDate,
                "전화번호를 입력해주세요."
        );
    }

    @DisplayName("전화번호가 빈 문자열이면 예약할 수 없다")
    @Test
    void rejectReservationWithEmptyPhoneNumber() {
        LocalDate reservationDate = TODAY.plusDays(1);

        assertReservationRejected(
                reservationRequest("B-5", reservationDate, reservationDate, ""),
                "B-5",
                reservationDate,
                "전화번호를 입력해주세요."
        );
    }

    @DisplayName("전화번호가 공백 문자열이면 예약할 수 없다")
    @Test
    void rejectReservationWithBlankPhoneNumber() {
        LocalDate reservationDate = TODAY.plusDays(1);

        assertReservationRejected(
                reservationRequest("B-6", reservationDate, reservationDate, "     "),
                "B-6",
                reservationDate,
                "전화번호를 입력해주세요."
        );
    }

    @DisplayName("허용된 형식이 아닌 전화번호로 예약할 수 없다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPhoneNumbers")
    void rejectReservationWithInvalidPhoneNumber(String phoneNumber, String siteNumber) {
        LocalDate reservationDate = TODAY.plusDays(1);

        assertReservationRejected(
                reservationRequest(siteNumber, reservationDate, reservationDate, phoneNumber),
                siteNumber,
                reservationDate,
                "유효한 전화번호가 아닙니다."
        );
    }

    static Stream<Arguments> invalidPhoneNumbers() {
        return Stream.of(
                Arguments.of("12345678910", "B-7"),
                Arguments.of("011-1234-5678", "B-8"),
                Arguments.of("010-12345678", "B-9"),
                Arguments.of("0101234-5678", "B-10"),
                Arguments.of("010-123-5678", "B-11"),
                Arguments.of("010-1234-567A", "B-12")
        );
    }

    private void assertReservationRejected(
            Map<String, Object> request,
            String siteNumber,
            LocalDate reservationDate,
            String expectedMessage
    ) {
        Response createResponse = RestAssured.given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations");

        List<String> reservedSiteNumbers = RestAssured.given()
                .queryParam("date", reservationDate.toString())
                .when()
                .get("/api/reservations")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("siteNumber", String.class);

        assertAll(
                () -> assertEquals(400, createResponse.statusCode()),
                () -> assertEquals(expectedMessage, createResponse.jsonPath().getString("message")),
                () -> assertFalse(reservedSiteNumbers.contains(siteNumber))
        );
    }

    private Map<String, Object> reservationRequest(
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return reservationRequest(siteNumber, startDate, endDate, "010-1234-5678");
    }

    private Map<String, Object> reservationRequest(
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate,
            String phoneNumber
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerName", "홍길동");
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("siteNumber", siteNumber);
        request.put("phoneNumber", phoneNumber);
        return request;
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
