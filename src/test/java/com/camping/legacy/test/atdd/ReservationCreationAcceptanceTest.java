package com.camping.legacy.test.atdd;

import com.camping.legacy.service.ReservationService;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.concurrent.*;

import static com.camping.legacy.test.atdd.testfixture.ReservationCreationTestFixture.defaultRequest;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

@Sql(scripts = "/sql/create-campsites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clear-reservations.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("예약 생성 테스트")
public class ReservationCreationAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private ReservationService reservationService;

    /**
     * Scenario: 예약을 생성한다.
     * <p>
     * Given A-1 사이트는 공실이다.
     * <p>
     * When 고객이 30일 이내의 예약기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약이 성공적으로 생성된다
     * and HTTP 상태 코드는 201이다
     * and 예약 상태는 "CONFIRMED"이다
     * and 예약 완료 시 6자리 영숫자 확인 코드 자동 생성된다.
     */
    @Test
    @DisplayName("30일 이내의 예약기간에 예약시, 예약이 생성된다.")
    void 정상적인_예약_생성_테스트() {

        // When: 고객이 오늘로부터 30일 이내 A-1 사이트를 예약한다
        var request = defaultRequest(now().plusDays(4), now().plusDays(6));

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

    /**
     * Scenario: 과거 날짜로 예약 생성 시, 예약 생성에 실패한다.
     * <p>
     * Given A-1 사이트는 공실이다.
     * <p>
     * When 고객이 과거날짜로 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     * and HTTP 상태 코드는 400이다
     */
    @Test
    @DisplayName("과거 날짜로 예약 신청시, 예약 생성이 실패한다.")
    void 과거_날짜로_예약_생성_시_에러() {

        // When: 고객이 과거 날짜로 A-1 사이트를 예약한다
        var request = defaultRequest(now().minusDays(1), now().plusDays(1));

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
     * Scenario: 30일 이후 날짜로 예약 생성 시, 예약 생성에 실패한다.
     * <p>
     * Given A-1 사이트는 공실이다.
     * <p>
     * When 고객이 30일 이후 날짜로 예약기간을 설정하여, A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     * and HTTP 상태 코드는 400이다
     */
    @Test
    @DisplayName("30일 이후 날짜로 예약 신청시, 예약 생성이 실패한다.")
    void 삼십일_이후_날짜로_예약_생성_시_에러() {

        // When: 고객이 30일 이후 날짜로 A-1 사이트를 예약한다
        var request = defaultRequest(
                now().plusDays(1),
                now().plusDays(30).plusSeconds(1)); // 30일 1초 이후

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
     * Scenario: 종료일이 시작일보다 이전인 경우, 예약 생성에 실패한다.
     * <p>
     * Given A-1은 공실이다.
     * <p>
     * When 고객이 예약 종료일을 예약시작일 이전으로 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     * and HTTP 상태 코드는 400이다
     */
    @Test
    @DisplayName("종료일이 시작일보다 이전인 경우, 예약 생성이 실패한다.")
    void 종료일이_시작일보다_이전인_경우_에러() {

        // When: 고객이 잘못된 날짜 순서로 A-1 사이트를 예약한다
        var request = defaultRequest(
                now().plusDays(10),
                now().plusDays(8)); // 종료일이 시작일보다 이전

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
     * Scenario: 고객명이 빈 문자열인 경우, 예약 생성에 실패한다.
     * <p>
     * Given A-1은 공실이다.
     * <p>
     * When 고객이 고객명을 빈값으로 입력하여 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     */
    @Test
    @DisplayName("고객명이 빈 문자열인 경우, 예약 생성이 실패한다.")
    void 고객명이_빈_문자열인_경우_에러() {

        // When: 고객이 빈 이름으로 A-1 사이트를 예약한다
        var request = defaultRequest();
        request.put("customerName", ""); // 빈 문자열

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
     * Scenario: 존재하지 않는 사이트로 예약 생성 시 에러
     * <p>
     * Given 사이트 Z-1이 존재하지 않는다
     * <p>
     * When 고객이 Z-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     */
    @Test
    @DisplayName("존재하지 않는 사이트로 예약 신청시, 예약 생성이 실패한다.")
    void 존재하지_않는_사이트로_예약_생성_시_에러() {

        // When: 고객이 존재하지 않는 사이트로 예약한다
        var request = defaultRequest();
        request.put("siteNumber", "Z-1"); // 존재하지 않는 사이트

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
    }

    /**
     * Scenario: 중복 예약 방지
     * <p>
     * Given 고객 고길동의 전화번호가 010-1234-5678이다.
     * and 고길동이 현재로부터 3일후~5일후까지 A-1 사이트를 예약했다.
     * and 다른 고객 김철수가 전화번호 010-2345-6789를 가지고 있다
     * <p>
     * When 김철수가 전화번호 전화번호 010-2345-6789로 고길동과 동일기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약 생성이 실패한다
     */
    @Test
    @DisplayName("중복 예약 방지 - 겹치는 기간으로 예약 신청시, 예약 생성이 실패한다.")
    void 중복_예약_방지() {

        // Given: 고길동이 A-1 사이트 예약 생성했다.
        var 예약신청_고길동 = defaultRequest("고길동", "010-1234-5678", "A-1");
        given()
                .contentType(JSON)
                .body(예약신청_고길동)
                .when()
                .post("/api/reservations");

        // When
        var 예약신청_김철수 = defaultRequest("김철수", "010-2345-6789", "A-1");
        var response = given()
                .contentType(JSON)
                .body(예약신청_김철수)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
    }

    /**
     * Scenario: 동시성 제어
     * <p>
     * Given A-1 사이트가 공실이다.
     * <p>
     * When 동시에 여러 고객이 동일 예약기간으로 A-1 사이트를 예약한다
     * <p>
     * Then 단 하나의 예약만 성공한다
     * and 나머지 예약들은 실패한다
     */
    @Test
    @DisplayName("동시성 제어 - 동시에 여러 고객이 예약 신청시, 하나만 성공한다.")
    void 동시성_제어() throws Exception {

        // when: 동시에 신청한다.
        var executor = Executors.newFixedThreadPool(2);

        var 예약신청_고길동 = defaultRequest("고길동", "010-1234-5678", "A-1");
        var 예약신청_김철수 = defaultRequest("김철수", "010-2345-6789", "A-1");

        var latchToCreateReservation = new CountDownLatch(1);

        List<ExtractableResponse<Response>> responses;
        try {
            var futures = List.of(예약신청_고길동, 예약신청_김철수).stream()
                    .map(request ->
                            CompletableFuture.supplyAsync(() -> {
                                try {
                                    latchToCreateReservation.await();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                return given()
                                        .contentType(JSON)
                                        .body(request)
                                        .when()
                                        .post("/api/reservations")
                                        .then()
                                        .extract();
                            }, executor)
                    ).toList();

            latchToCreateReservation.countDown(); // 두 스레드를 동시에 출발시킴

            responses = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }

        // Then: 단 하나의 예약만 성공한다 & 나머지 예약은 실패한다.
        SoftAssertions.assertSoftly(softly -> {
           softly.assertThat(responses.stream().map(ExtractableResponse::statusCode).toList())
                   .containsExactlyInAnyOrder(CREATED.value(), CONFLICT.value());
           softly.assertThat(reservationService.getAllReservations().size()).isEqualTo(1);
        });
    }
}
