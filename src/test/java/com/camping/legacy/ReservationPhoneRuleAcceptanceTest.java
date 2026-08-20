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

/**
 * T-2 인수 테스트 — docs/acceptance-criteria.md의 AC-4.
 *
 * AC-4는 아직 구현이 없으므로 거부 테스트 셋(누락·빈 문자열·공백만)이 지금 실패해야 한다.
 * 유효 전화번호는 기존 동작이라 지금 통과해야 하고, 구현 후에도 통과해야 한다.
 * "없음"은 세 형태(필드 누락·빈 문자열·공백만)라 각각 단언한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationPhoneRuleAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // ── AC-4 전화번호는 필수다 ──────────────────────────────────────

    @Test
    @DisplayName("AC-4 phoneNumber 필드가 없으면 거부된다")
    void phoneMissing_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("B-11", null))
        .when().post("/api/reservations")
        .then().statusCode(409);
        // 거부 문구는 아직 확정되지 않았다(acceptance-criteria.md Q-3) — 상태 코드만 단언한다.
    }

    @Test
    @DisplayName("AC-4 phoneNumber가 빈 문자열이면 거부된다")
    void phoneEmpty_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("B-12", ""))
        .when().post("/api/reservations")
        .then().statusCode(409);
    }

    @Test
    @DisplayName("AC-4 phoneNumber가 공백만이면 거부된다")
    void phoneBlank_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("B-13", "   "))
        .when().post("/api/reservations")
        .then().statusCode(409);
    }

    @Test
    @DisplayName("AC-4 유효한 전화번호면 예약된다")
    void phonePresent_accepted() {
        given().contentType(ContentType.JSON)
                .body(reservation("B-14", "010-1234-5678"))
        .when().post("/api/reservations")
        .then().statusCode(201);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────

    /** 시드가 DATEADD 상대 날짜라 고정 날짜를 쓸 수 없다. 실행 시각의 오늘을 기준으로 잡는다. */
    private LocalDate plusDays(int days) {
        return LocalDate.now().plusDays(days);
    }

    /** phoneNumber가 null이면 키 자체를 넣지 않는다 — "필드 누락"을 그대로 재현한다. */
    private Map<String, Object> reservation(String siteNumber, String phoneNumber) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("siteNumber", siteNumber);
        body.put("startDate", plusDays(1).toString());
        body.put("endDate", plusDays(1).toString());
        body.put("customerName", "테스터");
        if (phoneNumber != null) {
            body.put("phoneNumber", phoneNumber);
        }
        return body;
    }
}
