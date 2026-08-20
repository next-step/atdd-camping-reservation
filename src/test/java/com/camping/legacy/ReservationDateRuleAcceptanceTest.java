package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * T-1 인수 테스트 — docs/acceptance-criteria.md의 AC-1, AC-2, AC-3.
 *
 * AC-1은 아직 구현이 없으므로 leadTime31_rejected 가 지금 실패해야 한다.
 * 나머지는 이 티켓에서 바꾸지 않는 동작이라 지금 통과해야 하고, 구현 후에도 통과해야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationDateRuleAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // ── AC-1 시작일은 오늘로부터 30일 이내 ──────────────────────────

    @Test
    @DisplayName("AC-1 시작일이 오늘로부터 30일이면 예약된다")
    void leadTime30_accepted() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-10", plusDays(30), plusDays(30)))
        .when().post("/api/reservations")
        .then().statusCode(201);
    }

    @Test
    @DisplayName("AC-1 시작일이 오늘로부터 31일이면 거부된다")
    void leadTime31_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-11", plusDays(31), plusDays(31)))
        .when().post("/api/reservations")
        .then().statusCode(409);
        // 거부 문구는 아직 확정되지 않았다(acceptance-criteria.md Q-2) — 상태 코드만 단언한다.
    }

    // ── AC-2 시작일은 과거일 수 없다 (기존 동작) ────────────────────

    @Test
    @DisplayName("AC-2 시작일이 어제면 거부된다")
    void pastStartDate_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-12", plusDays(-1), plusDays(-1)))
        .when().post("/api/reservations")
        .then().statusCode(409)
                .body("message", equalTo("과거 날짜로 예약할 수 없습니다."));
    }

    // ── AC-3 숙박 길이는 최대 30일 (기존 동작) ──────────────────────

    @Test
    @DisplayName("AC-3 숙박 길이가 30일이면 예약된다")
    void stay30Days_accepted() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-13", plusDays(1), plusDays(31)))
        .when().post("/api/reservations")
        .then().statusCode(201);
    }

    @Test
    @DisplayName("AC-3 숙박 길이가 31일이면 거부된다")
    void stay31Days_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-14", plusDays(1), plusDays(32)))
        .when().post("/api/reservations")
        .then().statusCode(409)
                .body("message", equalTo("예약 기간은 최대 30일입니다."));
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────

    /** 시드가 DATEADD 상대 날짜라 고정 날짜를 쓸 수 없다. 실행 시각의 오늘을 기준으로 잡는다. */
    private LocalDate plusDays(int days) {
        return LocalDate.now().plusDays(days);
    }

    private Map<String, Object> reservation(String siteNumber, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("siteNumber", siteNumber);
        body.put("startDate", startDate.toString());
        body.put("endDate", endDate.toString());
        body.put("customerName", "테스터");
        body.put("phoneNumber", "010-1234-5678");
        return body;
    }
}
