package com.camping.legacy.acceptance;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReservationAcceptanceTest.FixedClockConfiguration.class)
class ReservationAcceptanceTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 18);

    @LocalServerPort
    private int port;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CampsiteRepository campsiteRepository;

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

    @Nested
    @DisplayName("예약 변경 전화번호 조건")
    class UpdatingReservationPhoneNumber {

        private static final String ORIGINAL_PHONE_NUMBER = "010-1111-2222";

        @DisplayName("필수 조건이나 형식 조건을 위반한 전화번호로 예약을 변경할 수 없다")
        @ParameterizedTest(name = "{0}")
        @MethodSource("rejectedPhoneNumberUpdates")
        void rejectInvalidPhoneNumberUpdate(
                String caseName,
                String siteNumber,
                boolean includePhoneNumber,
                String phoneNumber,
                String expectedMessage
        ) {
            LocalDate reservationDate = TODAY.plusDays(1);
            Response createdReservation = createReservation(siteNumber, reservationDate);
            Long reservationId = createdReservation.jsonPath().getLong("id");
            String confirmationCode = createdReservation.jsonPath().getString("confirmationCode");
            Map<String, Object> updateRequest = new HashMap<>();
            if (includePhoneNumber) {
                updateRequest.put("phoneNumber", phoneNumber);
            }

            Response updateResponse = updateReservation(
                    reservationId,
                    confirmationCode,
                    updateRequest
            );
            Response persistedReservation = getReservation(reservationId);

            assertAll(
                    () -> assertEquals(400, updateResponse.statusCode()),
                    () -> assertEquals(expectedMessage, updateResponse.jsonPath().getString("message")),
                    () -> assertEquals(
                            ORIGINAL_PHONE_NUMBER,
                            persistedReservation.jsonPath().getString("phoneNumber")
                    )
            );
        }

        static Stream<Arguments> rejectedPhoneNumberUpdates() {
            return Stream.of(
                    Arguments.of("전화번호 필드 누락", "B-1", false, null, "전화번호를 입력해주세요."),
                    Arguments.of("전화번호 null", "B-2", true, null, "전화번호를 입력해주세요."),
                    Arguments.of("전화번호 빈 문자열", "B-3", true, "", "전화번호를 입력해주세요."),
                    Arguments.of("전화번호 공백 문자열", "B-4", true, "     ", "전화번호를 입력해주세요."),
                    Arguments.of("010이 아닌 접두사", "B-5", true, "011-1234-5678", "유효한 전화번호가 아닙니다."),
                    Arguments.of("하이픈 일부 누락", "B-6", true, "010-12345678", "유효한 전화번호가 아닙니다."),
                    Arguments.of("자릿수 부족", "B-7", true, "010-123-5678", "유효한 전화번호가 아닙니다."),
                    Arguments.of("숫자 외 문자 포함", "B-8", true, "010-1234-567A", "유효한 전화번호가 아닙니다."),
                    Arguments.of("010이 아닌 숫자 형식", "B-9", true, "12345678910", "유효한 전화번호가 아닙니다.")
            );
        }

        @DisplayName("허용된 전화번호 형식으로 예약을 변경할 수 있다")
        @ParameterizedTest(name = "{0}")
        @MethodSource("allowedPhoneNumberUpdates")
        void updateReservationWithValidPhoneNumber(
                String caseName,
                String siteNumber,
                String phoneNumber
        ) {
            LocalDate reservationDate = TODAY.plusDays(1);
            Response createdReservation = createReservation(siteNumber, reservationDate);
            Long reservationId = createdReservation.jsonPath().getLong("id");
            String confirmationCode = createdReservation.jsonPath().getString("confirmationCode");
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("phoneNumber", phoneNumber);

            Response updateResponse = updateReservation(
                    reservationId,
                    confirmationCode,
                    updateRequest
            );
            Response persistedReservation = getReservation(reservationId);

            assertAll(
                    () -> assertEquals(200, updateResponse.statusCode()),
                    () -> assertEquals(phoneNumber, updateResponse.jsonPath().getString("phoneNumber")),
                    () -> assertEquals(
                            phoneNumber,
                            persistedReservation.jsonPath().getString("phoneNumber")
                    )
            );
        }

        static Stream<Arguments> allowedPhoneNumberUpdates() {
            return Stream.of(
                    Arguments.of("하이픈 형식", "B-10", "010-2222-3333"),
                    Arguments.of("숫자만 형식", "B-11", "01022223333")
            );
        }

        private Response createReservation(String siteNumber, LocalDate reservationDate) {
            Response response = RestAssured.given()
                    .contentType(JSON)
                    .body(reservationRequest(
                            siteNumber,
                            reservationDate,
                            reservationDate,
                            ORIGINAL_PHONE_NUMBER
                    ))
                    .when()
                    .post("/api/reservations");
            assertEquals(201, response.statusCode());
            return response;
        }

        private Response updateReservation(
                Long reservationId,
                String confirmationCode,
                Map<String, Object> updateRequest
        ) {
            return RestAssured.given()
                    .contentType(JSON)
                    .queryParam("confirmationCode", confirmationCode)
                    .body(updateRequest)
                    .when()
                    .put("/api/reservations/{id}", reservationId);
        }

        private Response getReservation(Long reservationId) {
            return RestAssured.given()
                    .when()
                    .get("/api/reservations/{id}", reservationId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
        }
    }

    @Nested
    @DisplayName("취소한 예약 자리의 재예약")
    class RebookingCancelledReservation {

        @DisplayName("확정된 예약과 같은 사이트와 날짜에는 새 예약을 만들 수 없다")
        @Test
        void rejectRebookingConfirmedReservation() {
            LocalDate reservationDate = TODAY.plusDays(1);
            Long existingReservationId = saveReservation("B-13", reservationDate, "CONFIRMED");

            Response rebookingResponse = createReservation("B-13", reservationDate);
            List<Map<String, Object>> storedReservations = getReservations(reservationDate);

            assertAll(
                    () -> assertEquals(409, rebookingResponse.statusCode()),
                    () -> assertEquals(
                            "해당 기간에 이미 예약이 존재합니다.",
                            rebookingResponse.jsonPath().getString("message")
                    ),
                    () -> assertEquals(1, storedReservations.size()),
                    () -> assertEquals(existingReservationId.intValue(), storedReservations.getFirst().get("id")),
                    () -> assertEquals("CONFIRMED", storedReservations.getFirst().get("status"))
            );
        }

        @DisplayName("취소된 예약과 같은 사이트와 날짜에는 새 예약을 만들 수 있다")
        @ParameterizedTest(name = "{0} 상태")
        @CsvSource({
                "CANCELLED, B-14",
                "CANCELLED_SAME_DAY, B-15"
        })
        void allowRebookingCancelledReservation(String cancelledStatus, String siteNumber) {
            LocalDate reservationDate = TODAY.plusDays(1);
            Long cancelledReservationId = saveReservation(siteNumber, reservationDate, cancelledStatus);

            Response rebookingResponse = createReservation(siteNumber, reservationDate);
            List<Map<String, Object>> storedReservations = getReservations(reservationDate);
            Object rebookedReservationId = rebookingResponse.jsonPath().get("id");
            List<String> storedStatuses = storedReservations.stream()
                    .map(reservation -> (String) reservation.get("status"))
                    .toList();

            assertAll(
                    () -> assertEquals(201, rebookingResponse.statusCode()),
                    () -> assertNotNull(rebookedReservationId),
                    () -> assertEquals("CONFIRMED", rebookingResponse.jsonPath().getString("status")),
                    () -> assertNotEquals(
                            cancelledReservationId.intValue(),
                            rebookedReservationId
                    ),
                    () -> assertEquals(2, storedReservations.size()),
                    () -> assertTrue(storedStatuses.contains(cancelledStatus)),
                    () -> assertTrue(storedStatuses.contains("CONFIRMED"))
            );
        }

        private Long saveReservation(String siteNumber, LocalDate reservationDate, String status) {
            Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber).orElseThrow();
            Reservation reservation = new Reservation(
                    "기존 예약자",
                    reservationDate,
                    reservationDate,
                    campsite
            );
            reservation.setPhoneNumber("010-1234-5678");
            reservation.setStatus(status);
            reservation.setConfirmationCode("T4TEST");
            return reservationRepository.save(reservation).getId();
        }

        private Response createReservation(String siteNumber, LocalDate reservationDate) {
            return RestAssured.given()
                    .contentType(JSON)
                    .body(reservationRequest(siteNumber, reservationDate, reservationDate))
                    .when()
                    .post("/api/reservations");
        }

        private List<Map<String, Object>> getReservations(LocalDate reservationDate) {
            return RestAssured.given()
                    .queryParam("date", reservationDate.toString())
                    .when()
                    .get("/api/reservations")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("$");
        }
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
