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
import static org.hamcrest.Matchers.notNullValue;

/**
 * F-6 인수 테스트 — docs/acceptance-criteria.md의 AC-7.
 *
 * 생성·수정 응답의 createdAt이 채워져야 한다. 저장은 되는데(@PrePersist) 응답 손 매핑이
 * 빠뜨리는 것이라, 두 테스트 모두 지금 실패해야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationCreatedAtAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("AC-7 생성(POST) 응답의 createdAt이 채워진다")
    void createResponse_hasCreatedAt() {
        given().contentType(ContentType.JSON)
                .body(reservation("A-18", plusDays(6), plusDays(7)))
        .when().post("/api/reservations")
        .then().statusCode(201)
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("AC-7 수정(PUT) 응답의 createdAt이 채워진다")
    void updateResponse_hasCreatedAt() {
        Response created = given().contentType(ContentType.JSON)
                .body(reservation("A-19", plusDays(8), plusDays(9)))
        .when().post("/api/reservations")
        .then().statusCode(201)
                .extract().response();

        long id = created.jsonPath().getLong("id");
        String code = created.jsonPath().getString("confirmationCode");

        given().contentType(ContentType.JSON)
                .queryParam("confirmationCode", code)
                .body(Map.of("customerName", "수정손님"))
        .when().put("/api/reservations/" + id)
        .then().statusCode(200)
                .body("createdAt", notNullValue());
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
