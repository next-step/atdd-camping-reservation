package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
 * T-3 인수 테스트 — docs/acceptance-criteria.md의 AC-5, AC-6.
 *
 * AC-6은 아직 구현이 없으므로 취소 후 재예약 테스트 둘이 마지막 걸음에서 지금 실패해야 한다.
 * AC-5(중복 거부)는 기존 동작이라 지금 통과해야 하고, 구현 후에도 통과해야 한다.
 * 여러 걸음을 밟는 테스트라 걸음마다 단언한다 — 앞 걸음의 조용한 실패가 원인을 가리지 않게.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationConflictRuleAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // ── AC-5 동일 사이트·기간 중복 예약 불가 (기존 동작) ────────────

    @Test
    @DisplayName("AC-5 같은 사이트 같은 기간에 두 번째 예약은 거부된다")
    void duplicate_rejected() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-15", plusDays(2), plusDays(3), "첫손님"))
        .when().post("/api/reservations")
        .then().statusCode(201);

        given().contentType(ContentType.JSON)
                .body(reservation("A-15", plusDays(2), plusDays(3), "둘째손님"))
        .when().post("/api/reservations")
        .then().statusCode(409)
                .body("message", equalTo("해당 기간에 이미 예약이 존재합니다."));
    }

    // ── AC-6 취소된 예약은 중복 체크에서 제외 ───────────────────────

    @Test
    @DisplayName("AC-6 취소한 자리에 같은 기간으로 다시 예약된다")
    void cancelledSlot_rebookable() {
        Response created = given().contentType(ContentType.JSON)
                .body(reservation("A-16", plusDays(4), plusDays(5), "취소할손님"))
        .when().post("/api/reservations")
        .then().statusCode(201)
                .extract().response();

        long id = created.jsonPath().getLong("id");
        String code = created.jsonPath().getString("confirmationCode");

        given().queryParam("confirmationCode", code)
        .when().delete("/api/reservations/" + id)
        .then().statusCode(200);

        given()
        .when().get("/api/reservations/" + id)
        .then().statusCode(200)
                .body("status", equalTo("CANCELLED"));

        given().contentType(ContentType.JSON)
                .body(reservation("A-16", plusDays(4), plusDays(5), "새손님"))
        .when().post("/api/reservations")
        .then().statusCode(201);
    }

    @Test
    @DisplayName("AC-6 당일 취소(CANCELLED_SAME_DAY)한 자리도 다시 예약된다")
    void sameDayCancelledSlot_rebookable() {
        Response created = given().contentType(ContentType.JSON)
                .body(reservation("A-17", plusDays(0), plusDays(0), "당일취소손님"))
        .when().post("/api/reservations")
        .then().statusCode(201)
                .extract().response();

        long id = created.jsonPath().getLong("id");
        String code = created.jsonPath().getString("confirmationCode");

        given().queryParam("confirmationCode", code)
        .when().delete("/api/reservations/" + id)
        .then().statusCode(200);

        given()
        .when().get("/api/reservations/" + id)
        .then().statusCode(200)
                .body("status", equalTo("CANCELLED_SAME_DAY"));

        given().contentType(ContentType.JSON)
                .body(reservation("A-17", plusDays(0), plusDays(0), "새손님"))
        .when().post("/api/reservations")
        .then().statusCode(201);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────

    /** 시드가 DATEADD 상대 날짜라 고정 날짜를 쓸 수 없다. 실행 시각의 오늘을 기준으로 잡는다. */
    private LocalDate plusDays(int days) {
        return LocalDate.now().plusDays(days);
    }

    private Map<String, Object> reservation(String siteNumber, LocalDate startDate, LocalDate endDate, String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("siteNumber", siteNumber);
        body.put("startDate", startDate.toString());
        body.put("endDate", endDate.toString());
        body.put("customerName", name);
        body.put("phoneNumber", "010-1234-5678");
        return body;
    }
}
