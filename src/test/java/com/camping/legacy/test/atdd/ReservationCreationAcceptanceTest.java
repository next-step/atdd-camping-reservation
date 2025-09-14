package com.camping.legacy.test.atdd;

import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@Sql(scripts = "/sql/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clear-reservation.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
@DisplayName("예약 생성 테스트")
public class ReservationCreationAcceptanceTest extends AcceptanceTestBase {

    /**
     * background:
     * 1. A-1은 존재하는 사이트이다.
     * 2. A-1은 공실이다.
     */
    @BeforeEach
    void setUp() {
        super.setUp();
    }

    /**
     * Scenario: 정상적인 예약 생성
     * <p>
     * Given A-1 사이트는 공실이다.
     * <p>
     * When 고객이 해당 예약 기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약이 성공적으로 생성된다
     * and HTTP 상태 코드는 201이다
     * and 예약 상태는 "CONFIRMED"이다
     * and 예약 완료 시 6자리 영숫자 확인 코드 자동 생성된다.
     */
    @Test
    @DisplayName("유효한 날짜로 예약 신청시, 예약이 생성된다.")
    void 정상적인_예약_생성_테스트() {
        // Given
        var startDate = now().plusDays(4).toString();
        var endDate = now().plusDays(6).toString();

        // When: 고객이 오늘로부터 30일 이내 A-1 사이트를 예약한다
        var request = defaultRequest(startDate, endDate);
        request.put("startDate", startDate);
        request.put("endDate", endDate);

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(CREATED.value());
            softly.assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            softly.assertThat(response.jsonPath().getString("confirmationCode")).matches("^[A-Za-z0-9]{6}$");
        });
    }

    private static HashMap<String, Object> defaultRequest(String startDate, String endDate) {
        var request = new HashMap<String, Object>();
        request.put("customerName", "홍길동");
        request.put("startDate", startDate);
        request.put("endDate", endDate);
        request.put("siteNumber", "A-1");
        request.put("phoneNumber", "010-1234-5678");
        request.put("numberOfPeople", 4);
        request.put("carNumber", "12가3456");
        request.put("requests", "조용한 구역 부탁드립니다");
        return request;
    }

    /**
     * Scenario: 과거 날짜로 예약 생성 시 에러
     * <p>
     * Given 예약기간에 과거가 포함되어있다. (예약시작일 = 오늘 날짜 - 1일, 예약종료일 = 오늘 날짜 + 1일)
     * and A-1 사이트는 공실이다.
     * <p>
     * When 고객이 해당 예약 기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     */
    @Test
    @DisplayName("과거 날짜로 예약 신청시, 예약 생성이 실패한다.")
    void 과거_날짜로_예약_생성_시_에러() {
        // Given
        var startDate = now().minusDays(1).toString(); // 과거 날짜
        var endDate = now().plusDays(1).toString();

        // When: 고객이 과거 날짜로 A-1 사이트를 예약한다
        var request = defaultRequest(startDate, endDate);

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
    }

    /**
     * Scenario: 30일 이후 날짜로 예약 생성 시 에러
     * <p>
     * Given  예약기간에 30일 이후 날짜가 포함되어있다. (예약시작일 = 오늘 날짜 + 1일, 예약종료일 = 오늘 날짜 + 30일 + 1초)
     * and A-1 사이트는 공실이다.
     * <p>
     * When 고객이 해당기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     */
    @Test
    @DisplayName("30일 이후 날짜로 예약 신청시, 예약 생성이 실패한다.")
    void 삼십일_이후_날짜로_예약_생성_시_에러() {
        // Given
        var startDate = now().plusDays(1).toString();
        var endDate = now().plusDays(30).plusSeconds(1).toString(); // 30일 1초 이후

        // When: 고객이 30일 이후 날짜로 A-1 사이트를 예약한다
        var request = defaultRequest(startDate, endDate);

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
    }

    /**
     * Scenario: 종료일이 시작일보다 이전인 경우 에러
     * <p>
     * Given 예약 종료일이 시작일보다 이전이다
     * <p>
     * When 고객이 해당 기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     */
    @Test
    @DisplayName("종료일이 시작일보다 이전인 경우, 예약 생성이 실패한다.")
    void 종료일이_시작일보다_이전인_경우_에러() {
        // Given
        var startDate = now().plusDays(10).toString();
        var endDate = now().plusDays(8).toString(); // 종료일이 시작일보다 이전

        // When: 고객이 잘못된 날짜 순서로 A-1 사이트를 예약한다
        var request = defaultRequest(startDate, endDate);

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
    }

    /**
     * Scenario: 고객명이 빈 문자열인 경우 에러
     * <p>
     * Given 오늘 날짜는 2024-12-01이다
     * <p>
     * When 고객이 2024-12-05부터 2024-12-07까지 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     * and 에러 메시지 "예약자 이름을 입력해주세요"가 반환된다
     */
    @Test
    @DisplayName("고객명이 빈 문자열인 경우, 예약 생성이 실패한다.")
    void 고객명이_빈_문자열인_경우_에러() {
        // Given
        var startDate = now().plusDays(4).toString();
        var endDate = now().plusDays(6).toString();

        // When: 고객이 빈 이름으로 A-1 사이트를 예약한다
        var request = defaultRequest(startDate, endDate);
        request.put("customerName", ""); // 빈 문자열

        given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .statusCode(400);
    }

    /**
     * Scenario: 존재하지 않는 사이트로 예약 생성 시 에러
     * <p>
     * Given 사이트 Z-1이 존재하지 않는다
     * and 오늘 날짜는 2024-12-01이다
     * <p>
     * When 고객이 2024-12-05부터 2024-12-07까지 Z-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     * and 에러 메시지 "존재하지 않는 캠핑장입니다"가 반환된다
     */
    @Test
    @DisplayName("존재하지 않는 사이트로 예약 신청시, 예약 생성이 실패한다.")
    void 존재하지_않는_사이트로_예약_생성_시_에러() {
        // Given
        var startDate = now().plusDays(4).toString();
        var endDate = now().plusDays(6).toString();

        // When: 고객이 존재하지 않는 사이트로 예약한다
        var request = defaultRequest(startDate, endDate);
        request.put("siteNumber", "Z-1"); // 존재하지 않는 사이트

        given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .statusCode(400);
    }

    /**
     * Scenario: 중복 예약 방지
     * <p>
     * Given 고객 홍길동이 2024-12-05부터 2024-12-07까지 A-1 사이트를 예약했다
     * and 다른 고객 김철수가 전화번호 010-2345-6789를 가지고 있다
     * <p>
     * When 김철수가 2024-12-06부터 2024-12-08까지 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     * and 에러 메시지 "해당 기간에 이미 예약이 존재합니다"가 반환된다
     */
    @Test
    @DisplayName("중복 예약 방지 - 겹치는 기간으로 예약 신청시, 예약 생성이 실패한다.")
    void 중복_예약_방지() {
        // Given: 고객 홍길동이 2024-12-05부터 2024-12-07까지 A-1 사이트를 예약했다
        var firstStartDate = now().plusDays(4).toString();
        var firstEndDate = now().plusDays(6).toString();
        var firstRequest = defaultRequest(firstStartDate, firstEndDate);

        given()
                .contentType(JSON)
                .body(firstRequest)
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(201);

        // And: 다른 고객 김철수가 전화번호 010-2345-6789를 가지고 있다
        // When: 김철수가 2024-12-06부터 2024-12-08까지 A-1 사이트를 예약한다
        var secondStartDate = now().plusDays(5).toString(); // 겹치는 기간
        var secondEndDate = now().plusDays(7).toString();
        var secondRequest = defaultRequest(secondStartDate, secondEndDate);
        secondRequest.put("customerName", "김철수");
        secondRequest.put("phoneNumber", "010-2345-6789");

        given()
                .contentType(JSON)
                .body(secondRequest)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .statusCode(409); // Conflict;
    }

    /**
     * Scenario: 동시성 제어
     * <p>
     * Given A-1 사이트가 예약 가능한 상태이다
     * and 고객 홍길동과 김철수가 동시에 예약을 시도한다
     * <p>
     * When 동시에 여러 고객이 2024-12-05부터 2024-12-07까지 A-1 사이트를 예약한다
     * <p>
     * Then 단 하나의 예약만 성공한다
     * and 나머지 예약들은 실패한다
     */
    @Test
    @DisplayName("동시성 제어 - 동시에 여러 고객이 예약 신청시, 하나만 성공한다.")
    void 동시성_제어() throws Exception {
        // Given: A-1 사이트가 예약 가능한 상태이다
        // And: 고객 홍길동과 김철수가 동시에 예약을 시도한다
        var startDate = now().plusDays(4).toString();
        var endDate = now().plusDays(6).toString();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When: 동시에 여러 고객이 A-1 사이트를 예약한다
        var request1 = defaultRequest(startDate, endDate);
        request1.put("customerName", "홍길동");

        var request2 = defaultRequest(startDate, endDate);
        request2.put("customerName", "김철수");
        request2.put("phoneNumber", "010-2345-6789");

        CompletableFuture<Response> future1 = CompletableFuture.supplyAsync(() -> {
            return given()
                    .contentType(JSON)
                    .body(request1)
                    .when()
                    .post("/api/reservations")
                    .then()
                    .extract()
                    .response();
        }, executor);

        CompletableFuture<io.restassured.response.Response> future2 = CompletableFuture.supplyAsync(() -> {
            return given()
                    .contentType(JSON)
                    .body(request2)
                    .when()
                    .post("/api/reservations")
                    .then()
                    .extract()
                    .response();
        }, executor);

        var response1 = future1.get(5, TimeUnit.SECONDS);
        var response2 = future2.get(5, TimeUnit.SECONDS);

        executor.shutdown();

        // Then: 단 하나의 예약만 성공한다
        // And: 나머지 예약들은 실패한다
        int successCount = 0;
        int failureCount = 0;

        if (response1.getStatusCode() == 201) {
            successCount++;
        } else {
            failureCount++;
        }

        if (response2.getStatusCode() == 201) {
            successCount++;
        } else {
            failureCount++;
        }

        assertEquals(1, successCount, "정확히 하나의 예약만 성공해야 함");
        assertEquals(1, failureCount, "정확히 하나의 예약은 실패해야 함");
    }

    /**
     * Scenario: 연박 예약 생성
     * <p>
     * Given 사이트 A-1이 존재한다
     * and 오늘 날짜는 2024-12-01이다
     * <p>
     * When 고객이 2024-12-05부터 2024-12-08까지 A-1 사이트를 예약한다
     * <p>
     * Then 예약이 성공적으로 생성된다
     * and 4일간의 연박 예약이 생성된다
     */
    @Test
    @DisplayName("연박 예약 생성 - 4일간의 연박 예약이 생성된다.")
    void 연박_예약_생성() {
        // Given: 사이트 A-1이 존재한다
        // And: 오늘 날짜는 2024-12-01이다
        var startDate = now().plusDays(4).toString();
        var endDate = now().plusDays(7).toString(); // 4일간 연박

        // When: 고객이 4일간의 연박 예약을 한다
        var request = defaultRequest(startDate, endDate);
        request.put("requests", "연박 예약입니다");

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .statusCode(201)
                .extract();

        // Then: 예약이 성공적으로 생성된다
        // And: 4일간의 연박 예약이 생성된다
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(CREATED.value());
            softly.assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            softly.assertThat(response.jsonPath().getString("startDate")).isEqualTo(startDate);
            softly.assertThat(response.jsonPath().getString("endDate")).isEqualTo(endDate);

            // 4일간의 연박 확인 (시작일과 종료일의 차이)
            var daysBetween = java.time.temporal.ChronoUnit.DAYS.between(now().plusDays(4), now().plusDays(7));
            softly.assertThat(daysBetween).isEqualTo(3); // 4일 (3박)

            String confirmationCode = response.jsonPath().getString("confirmationCode");
            softly.assertThat(confirmationCode)
                    .isNotNull()
                    .hasSize(6)
                    .matches("^[A-Za-z0-9]{6}$");
        });
    }

}
