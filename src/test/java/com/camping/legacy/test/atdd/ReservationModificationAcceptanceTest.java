package com.camping.legacy.test.atdd;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.concurrent.*;

import static com.camping.legacy.test.atdd.testfixture.ReservationCreationTestFixture.defaultRequest;
import static com.camping.legacy.test.atdd.testfixture.ReservationModificationTestFixture.defaultModificationRequest;
import static com.camping.legacy.test.atdd.testfixture.ReservationModificationTestFixture.modificationRequest;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

@Sql(scripts = "/sql/modify-reservation_create-campsites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clear-reservations.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("예약 수정 테스트")
public class ReservationModificationAcceptanceTest extends AcceptanceTestBase {

    /**
     * Scenario: 정상적인 예약 수정
     * <p>
     * Given 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
     * <p>
     * When 고객이 동일한 확인코드로 예약을 수정한다
     * <p>
     * Then 예약 수정이 성공한다
     * and HTTP 상태 코드는 200이다
     * and 수정된 예약 정보가 반환된다.
     * and 예약 상태는 "CONFIRMED"로 유지된다
     */
    @Test
    @DisplayName("정상적인 예약 수정 - 확인코드가 일치하면 예약 수정이 성공한다.")
    void 정상적인_예약_수정_테스트() {

        // Given: 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
        var modifyRequest = defaultRequest();
        var createResponse = given()
                .contentType(JSON)
                .body(modifyRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        var reservationId = createResponse.jsonPath().getLong("id");

        // When: 고객이 동일한 확인코드로 날짜와 장소에 대해 예약을 수정한다
        var startDate = now().plusDays(10);
        var endDate = now().plusDays(12);
        var modificationRequest = modificationRequest(
                "A-2", startDate, endDate, "김흥국", "010-1111-2222");

        var response = given()
                .contentType(JSON)
                .body(modificationRequest)
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .put("/api/reservations/{id}", reservationId)
                .then()
                .log().all()
                .extract();

        // Then: 예약 수정이 성공한다
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(OK.value());
            softly.assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            softly.assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-2");
            softly.assertThat(response.jsonPath().getString("startDate")).isEqualTo(startDate.toLocalDate().toString());
            softly.assertThat(response.jsonPath().getString("endDate")).isEqualTo(endDate.toLocalDate().toString());
            softly.assertThat(response.jsonPath().getString("customerName")).isEqualTo("김흥국");
            softly.assertThat(response.jsonPath().getString("phoneNumber")).isEqualTo("010-1111-2222");
        });
    }

    /**
     * Scenario: 확인 코드가 일치하지 않는 경우 에러
     * <p>
     * Given 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
     * <p>
     * When 고객이 잘못된 확인코드로 예약을 수정 했다.
     * <p>
     * Then 예약 수정이 실패한다 (HTTP 상태 코드 400)
     */
    @Test
    @DisplayName("확인 코드가 일치하지 않는 경우 - 예약 수정이 실패한다.")
    void 확인_코드가_일치하지_않는_경우_에러() {

        // Given: 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
        var createRequest = defaultRequest();
        var createResponse = given()
                .contentType(JSON)
                .body(createRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        var reservationId = createResponse.jsonPath().getLong("id");

        // When: 고객이 잘못된 확인코드로 예약을 수정 했다.
        var modificationRequest = defaultModificationRequest();

        var response = given()
                .contentType(JSON)
                .body(modificationRequest)
                .queryParam("confirmationCode", "WRONG1")
                .when()
                .put("/api/reservations/{id}", reservationId)
                .then()
                .log().all()
                .extract();

        // Then: 예약 수정이 실패한다 (HTTP 상태 코드 400)
        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
    }

    /**
     * Scenario: 존재하지 않는 예약 ID로 수정 시 에러
     * <p>
     * Given 예약이 생성되었다.
     *  AND 예약 ID 999는 존재하지 않는다.
     * <p>
     * When 고객이 예약 ID 999를 수정한다
     * <p>
     * Then 예약 수정이 실패한다 (HTTP 상태 코드 404)
     */
    @Test
    @DisplayName("존재하지 않는 예약 ID로 수정시 - 예약 수정이 실패한다.")
    void 존재하지_않는_예약_ID로_수정_시_에러() {

        // Given: 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
        var createRequest = defaultRequest();
        var createResponse = given()
                .contentType(JSON)
                .body(createRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        // Given: 예약 ID 999는 존재하지 않는다.
        var nonExistentReservationId = 999L;

        // When: 고객이 예약 ID 999를 수정한다
        var modificationRequest = defaultModificationRequest();
        var response = given()
                .contentType(JSON)
                .body(modificationRequest)
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .put("/api/reservations/{id}", nonExistentReservationId)
                .then()
                .log().all()
                .extract();

        // Then: 예약 수정이 실패한다 (HTTP 상태 코드 404)
        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
    }

    /**
     * Scenario: 30일 이후 날짜로 수정 시 에러
     * <p>
     * Given 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
     * <p>
     * When 고객이 동일한 확인코드로, 30일 이후의 날짜로 예약을 수정했다.
     * <p>
     * Then 예약 수정이 실패한다 (HTTP 상태 코드 400)
     */
    @Test
    @DisplayName("30일 이후 날짜로 수정시 - 예약 수정이 실패한다.")
    void 삼십일_이후_날짜로_수정_시_에러() {

        // Given: 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
        var createRequest = defaultRequest();
        var createResponse = given()
                .contentType(JSON)
                .body(createRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        var reservationId = createResponse.jsonPath().getLong("id");

        // When: 고객이 동일한 확인코드로, 30일 이후의 날짜로 예약을 수정했다.
        var modificationRequest = modificationRequest(
                "A-1",
                now().plusDays(31),
                now().plusDays(33)
        );

        var response = given()
                .contentType(JSON)
                .body(modificationRequest)
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .put("/api/reservations/{id}", reservationId)
                .then()
                .log().all()
                .extract();

        // Then: 예약 수정이 실패한다 (HTTP 상태 코드 400)
        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
    }

    /**
     * Scenario: 존재하지 않는 사이트로 수정 시 에러
     * <p>
     * Given 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
     * And 사이트 Z-1은 존재하지 않는다
     * <p>
     * When 고객이 사이트 Z-1으로 예약을 수정한다.
     * <p>
     * Then 예약 수정이 실패한다 (HTTP 상태 코드 404)
     */
    @Test
    @DisplayName("존재하지 않는 사이트로 수정시 - 예약 수정이 실패한다.")
    void 존재하지_않는_사이트로_수정_시_에러() {

        // Given: 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
        var createRequest = defaultRequest();
        var createResponse = given()
                .contentType(JSON)
                .body(createRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        var reservationId = createResponse.jsonPath().getLong("id");

        // When: 고객이 사이트 Z-1으로 예약을 수정한다.
        var modificationRequest = modificationRequest("Z-1");

        var response = given()
                .contentType(JSON)
                .body(modificationRequest)
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .put("/api/reservations/{id}", reservationId)
                .then()
                .log().all()
                .extract();

        // Then: 예약 수정이 실패한다 (HTTP 상태 코드 404)
        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
    }

    /**
     * Scenario: 동시성 제어
     * <p>
     * Given 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
     * <p>
     * When 동시에 여러 요청으로 확인 코드 ABC123으로 예약을 수정한다
     * <p>
     * Then 단 하나의 수정만 성공한다 (HTTP 상태 코드 201)
     * and 나머지 수정 요청들은 실패한다 (HTTP 상태 코드 409)
     */
    @Test
    @DisplayName("동시성 제어 - 동시에 여러 요청으로 예약 수정시, 하나만 성공한다.")
    void 동시성_제어() throws Exception {

        // Given: 고객 홍길동이 A-1 사이트 예약을 생성하고 확인코드가 생성되었다.
        var createRequest = defaultRequest();
        var createResponse = given()
                .contentType(JSON)
                .body(createRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        var reservationId = createResponse.jsonPath().getLong("id");

        // When: 동시에 여러 요청으로 확인 코드로 예약을 수정한다
        var executor = Executors.newFixedThreadPool(2);

        var modificationRequest1 = modificationRequest(now().plusDays(10), now().plusDays(12));
        var modificationRequest2 = modificationRequest(now().plusDays(15), now().plusDays(17));

        var latchToModifyReservation = new CountDownLatch(1);

        List<ExtractableResponse<Response>> responses;
        try {
            var futures = List.of(modificationRequest1, modificationRequest2).stream()
                    .map(request ->
                            CompletableFuture.supplyAsync(() -> {
                                try {
                                    latchToModifyReservation.await();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                return  given()
                                        .contentType(JSON)
                                        .body(request)
                                        .queryParam("confirmationCode", confirmationCode)
                                        .when()
                                        .put("/api/reservations/{id}", reservationId)
                                        .then()
                                        .log().all()
                                        .extract();
                            }, executor)
                    ).toList();

            latchToModifyReservation.countDown(); // 두 스레드를 동시에 출발시킴

            responses = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }

        // Then: 단 하나의 수정만 성공한다 & 나머지 수정 요청은 실패한다.
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responses.stream().map(ExtractableResponse::statusCode).toList())
                    .containsExactlyInAnyOrder(OK.value(), CONFLICT.value());
        });
    }
}
