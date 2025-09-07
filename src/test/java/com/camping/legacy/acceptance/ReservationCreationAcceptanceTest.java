package com.camping.legacy.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;

import com.camping.legacy.acceptance.support.ReservationTestDataBuilder;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
class ReservationCreationAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 예약_생성_성공_시_예약_번호_받기() {
        // when - 고객이 A-2 캠핑 구역을 12월 25일부터 27일까지 예약하면
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 25);
        LocalDate endDate = LocalDate.of(currentYear, 12, 27);

        Map<String, Object> reservationRequest = new ReservationTestDataBuilder()
                .withSiteNumber("A-2")
                .withDates(startDate, endDate)
                .withName("김테스트")
                .withPhone("010-1234-5678")
                .build();

        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(reservationRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        // then - 예약이 완료되고 6자리 예약 확인 번호를 받는다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("김테스트");
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-2");
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(startDate.toString());
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(endDate.toString());
    }

    @Test
    void 여러_명이_동시에_예약할_때_한_명만_성공() {
        // when - 10명의 고객이 동시에 12월 25일부터 27일로 A-1 캠핑 구역을 예약하려고 하면
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 25);
        LocalDate endDate = LocalDate.of(currentYear, 12, 27);

        Map<String, Object> reservationRequest = new ReservationTestDataBuilder()
                .withSiteNumber("A-1")
                .withDates(startDate, endDate)
                .withName("동시테스트")
                .withPhone("010-1111-1111")
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int customerIndex = i;
            executorService.execute(() -> {
                try {
                    Map<String, Object> request = new ReservationTestDataBuilder()
                            .withSiteNumber("A-1")
                            .withDates(startDate, endDate)
                            .withName("동시테스트" + customerIndex)
                            .withPhone("010-1111-111" + customerIndex)
                            .build();

                    ExtractableResponse<Response> response = given()
                            .contentType("application/json")
                            .body(request)
                    .when()
                            .post("/api/reservations")
                    .then()
                            .extract();

                    synchronized (responses) {
                        responses.add(response);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executorService.shutdown();

        // then - 한 명만 예약에 성공하고 나머지 9명은 예약할 수 없다
        long successCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == CREATED.value())
                .count();

        long conflictCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == CONFLICT.value())
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(9);
        assertThat(responses).hasSize(10);
    }

    @Test
    void 이미_예약된_캠핑_구역은_예약_불가() {
        // given - B-1 캠핑 구역이 12월 25일부터 27일까지 이미 예약되어 있을 때
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 25);
        LocalDate endDate = LocalDate.of(currentYear, 12, 27);

        // 사전 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
                .withSiteNumber("B-1")
                .withDates(startDate, endDate)
                .withName("기존고객")
                .withPhone("010-9999-9999")
                .build();

        given()
                .contentType("application/json")
                .body(existingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());

        // 동일한 기간으로 새로운 예약 시도
        Map<String, Object> newReservation = new ReservationTestDataBuilder()
                .withSiteNumber("B-1")
                .withDates(startDate, endDate)
                .withName("새로운고객")
                .withPhone("010-8888-8888")
                .build();

        // when - 고객이 같은 기간으로 B-1 캠핑 구역을 예약하려고 하면
        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(newReservation)
        .when()
                .post("/api/reservations")
        .then()
                .extract();

        // then - "해당 기간에 이미 예약이 존재합니다"라는 안내 메시지가 나타난다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 단독_예약_시도_성공() {
        // when - B-8 캠핑 구역을 1명의 고객이 예약을 시도하면
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(7);

        Map<String, Object> singleReservation = new ReservationTestDataBuilder()
                .withSiteNumber("B-8")
                .withDates(startDate, endDate)
                .withName("단독예약고객")
                .withPhone("010-1000-0001")
                .build();

        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(singleReservation)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        // then - 동시성 문제 없이 성공한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("단독예약고객");
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
    }

    @Test
    void 확인_코드_6자리_정확성_검증() {
        // when - B-9 캠핑 구역에 예약을 생성하면
        LocalDate startDate = LocalDate.now().plusDays(8);
        LocalDate endDate = LocalDate.now().plusDays(10);

        Map<String, Object> reservationForCode = new ReservationTestDataBuilder()
                .withSiteNumber("B-9")
                .withDates(startDate, endDate)
                .withName("확인코드검증고객")
                .withPhone("010-2000-0001")
                .build();

        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(reservationForCode)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        // then - 확인 코드는 영문자 6자리여야 한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).hasSize(6);
        assertThat(confirmationCode).matches("[A-Z0-9]{6}"); // 영문 대문자와 숫자만
    }

    @Test
    void 다중_예약_생성_독립성_검증() {
        // when - 5개의 서로 다른 캠핑 구역을 연속으로 예약하면
        LocalDate startDate = LocalDate.now().plusDays(6);
        LocalDate endDate = LocalDate.now().plusDays(8);

        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> reservation = new ReservationTestDataBuilder()
                    .withSiteNumber("B-1" + i) // B-11, B-12, B-13, B-14, B-15
                    .withDates(startDate, endDate)
                    .withName("다중예약고객" + i)
                    .withPhone("010-3000-000" + i)
                    .build();

            ExtractableResponse<Response> response = given()
                    .contentType("application/json")
                    .body(reservation)
                    .when()
                        .post("/api/reservations")
                    .then()
                        .extract();

            responses.add(response);
        }

        // then - 모든 예약이 성공해야 한다
        for (int i = 0; i < responses.size(); i++) {
            ExtractableResponse<Response> response = responses.get(i);
            assertThat(response.statusCode()).isEqualTo(CREATED.value());
        }
    }
}
