package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * docs/acceptance-criteria.md 예약 관련 인수 테스트를 티켓 구분 없이 모아둔다.
 * 날짜는 실행 시점의 LocalDate.now() 기준 상대값으로 계산해 고정 날짜에 의존하지 않는다.
 * 실제 서버(HTTP)로 예약을 만들기 때문에 트랜잭션 롤백이 없다 — 격리는 먼 날짜나 서로 다른
 * 사이트로 피하지 않고, 매 테스트 전 @Sql로 reservations 테이블을 비워 보장한다.
 * 그래서 모든 테스트가 같은 사이트 번호(SITE)를 재사용해도 안전하다.
 * 티켓별 세부 내용(어떤 케이스가 왜 레드인지 등)은 각 @Nested 클래스 위에 적는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/cleanup-reservations.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ReservationAcceptanceTest {

    private static final String SITE = "A-1";

    @LocalServerPort
    int port;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * T-1: 시작일-오늘 30일 검증이 아직 구현되지 않아, 31일 뒤 거부 케이스는 현재 실패(레드)하는 것이 정상이다.
     */
    @Nested
    @DisplayName("T-1-1: 시작일이 오늘로부터 30일 이내여야 한다")
    class StartDateWithin30Days {

        @Test
        @DisplayName("오늘로부터 31일 뒤 시작이면 거부된다")
        void rejectsStartDateOver30DaysAhead() {
            LocalDate start = LocalDate.now().plusDays(31);
            LocalDate end = start.plusDays(1);

            given()
                .contentType(ContentType.JSON)
                .body(reservationJson(SITE, start, end, "over30"))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409)
                .body("message", containsString("30"));
        }

        @Test
        @DisplayName("오늘로부터 정확히 30일 뒤 시작이면 허용된다 (경계값)")
        void allowsStartDateExactly30DaysAhead() {
            LocalDate start = LocalDate.now().plusDays(30);
            LocalDate end = start.plusDays(1);

            given()
                .contentType(ContentType.JSON)
                .body(reservationJson(SITE, start, end, "boundary30"))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .body("customerName", equalTo("boundary30"));
        }

        @Test
        @DisplayName("오늘로부터 29일 뒤 시작이면 허용된다")
        void allowsStartDateUnder30DaysAhead() {
            LocalDate start = LocalDate.now().plusDays(29);
            LocalDate end = start.plusDays(1);

            given()
                .contentType(ContentType.JSON)
                .body(reservationJson(SITE, start, end, "under30"))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201);
        }
    }

    /**
     * T-1: T-1-1의 30일 검증을 구현하면서 이미 되던 검증(과거 날짜, 날짜 역전)이 깨지지 않는지 확인하는 회귀 테스트.
     */
    @Nested
    @DisplayName("T-1-2 회귀: 기존에 동작하던 날짜 검증이 깨지지 않아야 한다")
    class ExistingDateValidationRegression {

        @Test
        @DisplayName("과거 날짜로는 예약할 수 없다")
        void rejectsPastDate() {
            given()
                .contentType(ContentType.JSON)
                .body(reservationJson(SITE, LocalDate.now().minusDays(13), LocalDate.now().minusDays(11), "pastdate"))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409)
                .body("message", equalTo("과거 날짜로 예약할 수 없습니다."));
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전일 수 없다")
        void rejectsReversedDates() {
            LocalDate start = LocalDate.now().plusDays(10);
            LocalDate end = start.minusDays(5);

            given()
                .contentType(ContentType.JSON)
                .body(reservationJson(SITE, start, end, "reversed"))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409)
                .body("message", equalTo("종료일이 시작일보다 이전일 수 없습니다."));
        }
    }

    /**
     * T-1: T-1-1의 30일 검증을 구현한 뒤에도 확인 코드 형식(영숫자 6자리)이 유지되는지 확인하는 회귀 테스트.
     */
    @Nested
    @DisplayName("T-1-3 회귀: 확인 코드 형식이 유지되어야 한다")
    class ConfirmationCodeFormat {

        @Test
        @DisplayName("예약 완료 시 6자리 영숫자 대문자 확인 코드가 생성된다")
        void generatesSixCharAlphanumericConfirmationCode() {
            LocalDate start = LocalDate.now().plusDays(5);
            LocalDate end = start.plusDays(1);

            given()
                .contentType(ContentType.JSON)
                .body(reservationJson(SITE, start, end, "codecheck"))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .body("confirmationCode", matchesPattern("^[0-9A-Z]{6}$"));
        }
    }

    private String reservationJson(String siteNumber, LocalDate start, LocalDate end, String customerName) {
        return """
            {
              "siteNumber": "%s",
              "startDate": "%s",
              "endDate": "%s",
              "customerName": "%s"
            }
            """.formatted(siteNumber, start.format(FMT), end.format(FMT), customerName);
    }
}
