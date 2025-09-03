package com.camping.legacy;

import com.camping.legacy.dto.ReservationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationCreateAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * TODO:
     *  - db가 어디에 붙고 있는지 확인하기 (h2? 아니면 로컬 application-atdd.yml 설정해야할 것 같은데?)
     *  - objectMapper가 필요한건지 확인
     *  - testBackdoor 추가?
     */

    @Test
    @DisplayName("예약 생성을 성공한다")
    void create_success_returns_id() {
        // Given
        LocalDate start = LocalDate.of(2025, 9, 10);
        LocalDate end = LocalDate.of(2025, 9, 13);
        ReservationRequest req = new ReservationRequest(
                "홍길동",
                start,
                end,
                "A-3", // A-1 may be pre-booked by data.sql for these dates; choose available site
                "010-1234-5678",
                2,
                "12가3456",
                "조용한 자리 부탁"
        );
        // When
        Response res = given()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("customerName", equalTo("홍길동"))
                .body("siteNumber", equalTo("A-3"));
    }

    @Test
    @DisplayName("예약 생성에 실패한다 (이름 누락)")
    void create_fail_missing_name() throws Exception {
        LocalDate start = LocalDate.of(2025, 9, 10);
        LocalDate end = LocalDate.of(2025, 9, 13);
        ReservationRequest req = new ReservationRequest(
                " ", // 이름 누락/공백
                start,
                end,
                "A-5",
                "010-0000-0000",
                2,
                "11가1111",
                null
        );
        given()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations")
                .then()
                .statusCode(409)
                .body("message", containsString("예약자 이름을 입력해주세요."));
    }

    @Test
    @DisplayName("예약 생성에 실패한다 (과거 기간 선택)")
    void create_fail_past_period() throws Exception {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now().minusDays(3);
        ReservationRequest req = new ReservationRequest(
                "홍길동",
                start,
                end,
                "A-6",
                "010-9999-8888",
                2,
                null,
                null
        );
        given()
                .contentType(ContentType.JSON)
                .body(req)
                .then()
                .statusCode(409)
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("예약 생성에 실패한다 (종료일이 시작일보다 이전)")
    void create_fail_end_before_start() throws Exception {
        LocalDate start = LocalDate.of(2025, 9, 13);
        LocalDate end = LocalDate.of(2025, 9, 10);
        ReservationRequest req = new ReservationRequest(
                "홍길동",
                start,
                end,
                "A-7",
                "010-1111-2222",
                2,
                null,
                null
        );
        given()
                .contentType(ContentType.JSON)
                .body(req)
                .then()
                .statusCode(409)
                .body("message", containsString("종료일이 시작일보다 이전"));
    }

    @Test
    @DisplayName("예약 생성에 실패한다 (30일 초과 기간 선택)")
    void create_fail_over_30_days() throws Exception {
        LocalDate start = LocalDate.of(2025, 9, 10);
        LocalDate end = start.plusDays(31);
        ReservationRequest req = new ReservationRequest(
                "홍길동",
                start,
                end,
                "A-8",
                "010-3333-4444",
                2,
                null,
                null
        );
        given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .then().log().all()
                .statusCode(409)
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("예약 생성에 실패한다 (이미 예약된 사이트 동일 기간)")
    void create_fail_duplicate_same_period() throws Exception {
        LocalDate start = LocalDate.of(2025, 9, 10);
        LocalDate end = LocalDate.of(2025, 9, 13);
        // First create a reservation on a site
        ReservationRequest req1 = new ReservationRequest(
                "홍길동",
                start,
                end,
                "A-9",
                "010-5555-6666",
                2,
                null,
                null
        );
        given()
                .contentType(ContentType.JSON)
                .body(req1)
                .then()
                .statusCode(201)
                .body("message", notNullValue());

        // Try to create another reservation for the same site and overlapping dates
        ReservationRequest req2 = new ReservationRequest(
                "임꺽정",
                start,
                end,
                "A-9",
                "010-7777-8888",
                2,
                null,
                null
        );
        given()
                .contentType(ContentType.JSON)
                .body(req2)
                .then()
                .statusCode(409)
                .body("message", containsString("이미 예약"));
    }
}
