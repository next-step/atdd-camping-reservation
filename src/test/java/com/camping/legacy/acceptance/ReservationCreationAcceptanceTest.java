package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * docs/acceptance-criteria.md 기반 인수 테스트 초안.
 *
 * acceptance-criteria.md에 "질문"으로 남아 있는 경계(정확히 오늘+30일째, 시작일=오늘,
 * 전화번호 형식 유효성 판단 기준)는 답이 정해지지 않아 테스트를 만들지 않았다.
 * 답이 정해지면 추가한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationCreationAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("시작일이 오늘+29일이면 예약이 생성되고 확인 코드가 발급된다")
    void 시작일이_29일_이내면_예약된다() {
        ReservationRequest request = reservationRequest("A-15", 29, 30);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("confirmationCode", matchesPattern("^[A-Z0-9]{6}$"))
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @DisplayName("시작일이 오늘+31일이면 예약이 거부된다")
    void 시작일이_31일_이후면_거부된다() {
        ReservationRequest request = reservationRequest("A-16", 31, 32);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("예약 시작일은 오늘로부터 30일 이내여야합니다."));
    }


    @Test
    @DisplayName("시작일이 현재보다 미래면 예약이 생성되고 확인 코드가 발급된다")
    void 시작일이_미래면_예약된다() {
        ReservationRequest request = reservationRequest("A-17", 1, 2);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("confirmationCode", matchesPattern("^[A-Z0-9]{6}$"));
    }

    @Test
    @DisplayName("시작일이 현재보다 과거면 예약이 거부된다")
    void 시작일이_과거면_거부된다() {
        ReservationRequest request = reservationRequest("A-18", -1, 1);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("과거 날짜로 예약할 수 없습니다."));
    }

    @Test
    @DisplayName("시작일이 종료일보다 이전이면 예약이 생성되고 확인 코드가 발급된다")
    void 시작일이_종료일보다_이전이면_예약된다() {
        ReservationRequest request = reservationRequest("A-19", 2, 3);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("confirmationCode", matchesPattern("^[A-Z0-9]{6}$"));
    }

    @Test
    @DisplayName("시작일이 종료일보다 이후면 예약이 거부된다")
    void 시작일이_종료일보다_이후면_거부된다() {
        ReservationRequest request = reservationRequest("A-20", 5, 4);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("종료일이 시작일보다 이전일 수 없습니다."));
    }

    @Test
    @DisplayName("전화번호가 없으면 예약이 거부된다")
    void 전화번호가_없으면_거부된다() {
        ReservationRequest request = reservationRequest("B-1", 1, 2);
        request.setPhoneNumber(null);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요"));
    }

    @Test
    @DisplayName("전화번호가 빈 문자열이면 예약이 거부된다")
    void 전화번호가_빈문자열이면_거부된다() {
        ReservationRequest request = reservationRequest("B-2", 1, 2);
        request.setPhoneNumber("");

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요"));
    }


    @Test
    @DisplayName("전화번호가 공백이면 예약이 거부된다")
    void 전화번호가_공백이면_거부된다() {
        ReservationRequest request = reservationRequest("B-3", 1, 2);
        request.setPhoneNumber("   ");

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요"));
    }

    @Test
    @DisplayName("전화번호가 10~11자리 숫자면 예약이 생성되고 확인 코드가 발급된다")
    void 전화번호가_10에서_11자리_숫자면_예약된다() {
        ReservationRequest request = reservationRequest("B-4", 1, 2);
        request.setPhoneNumber("01012345678");

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("confirmationCode", matchesPattern("^[A-Z0-9]{6}$"));
    }

    @Test
    @DisplayName("취소된 예약과 겹치는 기간으로 예약하면 생성되고 확인 코드가 발급된다")
    void 취소된_예약과_겹치는_기간은_예약된다() {
        ReservationRequest first = reservationRequest("A-11", 3, 4);
        var created = RestAssured.given().contentType(ContentType.JSON).body(first)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .extract().response();

        RestAssured.given().queryParam("confirmationCode", created.path("confirmationCode").toString())
                .when().delete("/api/reservations/" + created.path("id").toString())
                .then().statusCode(200);

        ReservationRequest second = reservationRequest("A-11", 3, 4);
        RestAssured.given().contentType(ContentType.JSON).body(second)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("confirmationCode", matchesPattern("^[A-Z0-9]{6}$"));
    }

    @Test
    @DisplayName("확정된 예약과 겹치는 기간으로 예약하면 거부된다")
    void 확정된_예약과_겹치는_기간은_거부된다() {
        ReservationRequest first = reservationRequest("A-12", 3, 4);
        RestAssured.given().contentType(ContentType.JSON).body(first)
                .when().post("/api/reservations")
                .then().statusCode(201);

        ReservationRequest second = reservationRequest("A-12", 3, 4);
        RestAssured.given().contentType(ContentType.JSON).body(second)
                .when().post("/api/reservations")
                .then().statusCode(409)
                .body("message", equalTo("해당 기간에 이미 예약이 존재합니다."));
    }

    // === 상태 확장성 관련 테스트 ===

    @Test
    @DisplayName("당일취소(CANCELLED_SAME_DAY) 된 자리에 같은 기간으로 재예약하면 201")
    void 당일취소된_자리에_재예약하면_생성된다() {
        ReservationRequest first = reservationRequest("B-7", 0, 1);
        var created = RestAssured.given().contentType(ContentType.JSON).body(first)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .extract().response();

        RestAssured.given().queryParam("confirmationCode", created.path("confirmationCode").toString())
                .when().delete("/api/reservations/" + created.path("id").toString())
                .then().statusCode(200);

        ReservationRequest second = reservationRequest("B-7", 0, 1);
        RestAssured.given().contentType(ContentType.JSON).body(second)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("confirmationCode", matchesPattern("^[A-Z0-9]{6}$"));
    }

    @Test
    @DisplayName("예약 생성 시 status는 CONFIRMED이다")
    void 예약_생성시_상태는_CONFIRMED이다() {
        ReservationRequest request = reservationRequest("B-8", 1, 2);

        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @DisplayName("일반 취소 후 status는 CANCELLED이다")
    void 일반_취소_후_상태는_CANCELLED이다() {
        ReservationRequest request = reservationRequest("B-9", 3, 4);
        var created = RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .extract().response();

        String id = created.path("id").toString();
        String code = created.path("confirmationCode").toString();

        RestAssured.given().queryParam("confirmationCode", code)
                .when().delete("/api/reservations/" + id)
                .then().statusCode(200);

        RestAssured.given()
                .when().get("/api/reservations/" + id)
                .then().statusCode(200)
                .body("status", equalTo("CANCELLED"));
    }

    @Test
    @DisplayName("당일 취소 후 status는 CANCELLED_SAME_DAY이다")
    void 당일_취소_후_상태는_CANCELLED_SAME_DAY이다() {
        ReservationRequest request = reservationRequest("B-10", 0, 1);
        var created = RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(201)
                .extract().response();

        String id = created.path("id").toString();
        String code = created.path("confirmationCode").toString();

        RestAssured.given().queryParam("confirmationCode", code)
                .when().delete("/api/reservations/" + id)
                .then().statusCode(200);

        RestAssured.given()
                .when().get("/api/reservations/" + id)
                .then().statusCode(200)
                .body("status", equalTo("CANCELLED_SAME_DAY"));
    }

    @Test
    @DisplayName("status=null인 예약이 있으면 같은 기간 예약 시 충돌(409)로 간주해야 한다")
    void status가_null인_예약과_겹치면_충돌이다() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(6);

        // DB에 status=null인 예약을 직접 삽입
        Long campsiteId = jdbcTemplate.queryForObject(
                "SELECT id FROM campsites WHERE site_number = ?", Long.class, "B-11");
        jdbcTemplate.update(
                "INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NULL, ?, CURRENT_TIMESTAMP)",
                "ghost", start, end, start, campsiteId, "010-0000-0000", "NULL01");

        // 같은 사이트·기간으로 예약 → 충돌(409) 기대
        ReservationRequest request = reservationRequest("B-11", 5, 6);
        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409);
    }

    @Test
    @DisplayName("알 수 없는 상태(PENDING)인 예약이 있으면 같은 기간 예약 시 충돌(409)로 간주해야 한다")
    void 알수없는_상태의_예약과_겹치면_충돌이다() {
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end = LocalDate.now().plusDays(8);

        Long campsiteId = jdbcTemplate.queryForObject(
                "SELECT id FROM campsites WHERE site_number = ?", Long.class, "B-12");
        jdbcTemplate.update(
                "INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, CURRENT_TIMESTAMP)",
                "unknown", start, end, start, campsiteId, "010-0000-0000", "PEND01");

        ReservationRequest request = reservationRequest("B-12", 7, 8);
        RestAssured.given().contentType(ContentType.JSON).body(request)
                .when().post("/api/reservations")
                .then().statusCode(409);
    }

    private ReservationRequest reservationRequest(String siteNumber, long startOffsetDays, long endOffsetDays) {
        ReservationRequest request = new ReservationRequest();
        request.setSiteNumber(siteNumber);
        request.setStartDate(LocalDate.now().plusDays(startOffsetDays));
        request.setEndDate(LocalDate.now().plusDays(endOffsetDays));
        request.setCustomerName("tester");
        request.setPhoneNumber("010-1234-5678");
        return request;
    }
}
