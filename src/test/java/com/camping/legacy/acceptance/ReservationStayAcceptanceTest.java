package com.camping.legacy.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.CONFLICT;

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
class ReservationStayAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 중간_날짜에_다른_예약이_있으면_예약_불가() {
        // given - A-5 캠핑 구역이 12월 22일부터 23일까지 예약되어 있을 때
        int currentYear = LocalDate.now().getYear();
        LocalDate conflictDate = LocalDate.of(currentYear, 12, 22);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-5")
            .withDates(conflictDate, conflictDate.plusDays(1))
            .withName("기존예약고객")
            .withPhone("010-7777-7777")
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
            .post("/api/reservations")
            .then()
            .statusCode(CREATED.value());

        // when - 고객이 12월 20일부터 24일까지 예약하려고 하면
        LocalDate startDate = LocalDate.of(currentYear, 12, 20);
        LocalDate endDate = LocalDate.of(currentYear, 12, 24);

        Map<String, Object> newReservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-5")
            .withDates(startDate, endDate)
            .withName("연박예약고객")
            .withPhone("010-6666-6666")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(newReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 실패한다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 날짜_선택_오류_시_예약_불가() {
        // when - 고객이 체크아웃 날짜를 체크인 날짜보다 앞선 날로 선택하면
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 10);
        LocalDate endDate = LocalDate.of(currentYear, 12, 8);

        Map<String, Object> invalidReservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-7")
            .withDates(startDate, endDate)
            .withName("잘못된날짜고객")
            .withPhone("010-5555-5555")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(invalidReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 실패한다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    void 일부_날짜만_가능해도_전체_예약_불가() {
        // given - A-2 캠핑 구역이 12월 20일, 21일은 비어있지만 22일은 예약되어 있을 때
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 22);
        LocalDate endDate = LocalDate.of(currentYear, 12, 23);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-2")
            .withDates(startDate, endDate)
            .withName("부분충돌고객")
            .withPhone("010-3333-3333")
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
            .post("/api/reservations")
            .then()
            .statusCode(CREATED.value());

        // when - 고객이 12월 20일부터 22일까지 예약하려고 하면
        startDate = LocalDate.of(currentYear, 12, 20);
        endDate = LocalDate.of(currentYear, 12, 22);

        Map<String, Object> overlappingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-2")
            .withDates(startDate, endDate)
            .withName("부분예약고객")
            .withPhone("010-2222-2222")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(overlappingReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약할 수 없다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void _30일_이내만_예약_가능() {
        // when - 고객이 오늘로부터 31일 이후의 날짜로 예약하려고 하면
        LocalDate startDate = clockProvider.now().plusDays(31);
        LocalDate endDate = clockProvider.now().plusDays(33);

        Map<String, Object> futureReservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-8")
            .withDates(startDate, endDate)
            .withName("미래예약고객")
            .withPhone("010-4444-4444")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(futureReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - "예약은 30일 이내에만 가능합니다"라는 안내 메시지가 나타난다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약은 30일 이내에만 가능합니다");
    }

    @Test
    void _29일_후_예약은_성공() {
        // when - B-3 캠핑 구역을 고객이 오늘로부터 29일 후의 날짜로 예약하려고 하면
        LocalDate startDate = LocalDate.now().plusDays(29);
        LocalDate endDate = LocalDate.now().plusDays(30);

        Map<String, Object> validReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-3")
            .withDates(startDate, endDate)
            .withName("29일경계고객")
            .withPhone("010-1111-2222")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(validReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
    }

    @Test
    void _30일_후_예약은_성공() {
        // when - B-4 캠핑 구역을 고객이 오늘로부터 30일 후에 예약하려고 하면
        LocalDate startDate = LocalDate.now().plusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(31);

        Map<String, Object> validReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-4")
            .withDates(startDate, endDate)
            .withName("30일경계고객")
            .withPhone("010-2222-3333")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(validReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
    }

    @Test
    void 시작일과_종료일이_동일한_당일_예약_성공() {
        // when - B-5 캠핑 구역을 고객이 시작일과 종료일이 동일한 당일 예약을 시도하면
        LocalDate sameDate = LocalDate.now().plusDays(15);

        Map<String, Object> sameDayReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-5")
            .withDates(sameDate, sameDate)
            .withName("당일예약고객")
            .withPhone("010-3333-4444")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(sameDayReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
    }

    @Test
    void 오늘_날짜_예약_시도_성공() {
        // given - B-6 캠핑 구역을 고객이 오늘 날짜로 예약을 시도하면
        LocalDate today = LocalDate.now();

        Map<String, Object> todayReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-6")
            .withDates(today, today.plusDays(1))
            .withName("오늘예약고객")
            .withPhone("010-4444-5555")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(todayReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
    }

    @Test
    void 존재하지_않는_캠핑장_예약_시도_실패() {
        // when - 고객이 존재하지 않는 캠핑장으로 예약하려고 하면
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        Map<String, Object> invalidSiteReservation = new ReservationTestDataBuilder()
            .withSiteNumber("Z-99")  // 존재하지 않는 캠핑장
            .withDates(startDate, endDate)
            .withName("잘못된사이트고객")
            .withPhone("010-5555-6666")
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(invalidSiteReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - "존재하지 않는 캠핑장입니다" 오류가 발생한다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("존재하지 않는 캠핑장");
    }

    @Test
    void 최소_동시성_2명_예약_시도() {
        // when - 2명의 고객이 동시에 같은 기간으로 B-7 캠핑 구역을 예약하려고 하면
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            final int customerIndex = i;
            executorService.execute(() -> {
                try {
                    Map<String, Object> request = new ReservationTestDataBuilder()
                        .withSiteNumber("B-7")
                        .withDates(startDate, endDate)
                        .withName("최소동시고객" + customerIndex)
                        .withPhone("010-6666-777" + customerIndex)
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

        // then - 한 명만 예약에 성공하고 한 명은 실패한다
        long successCount = responses.stream()
            .mapToInt(ExtractableResponse::statusCode)
            .filter(status -> status == CREATED.value())
            .count();

        long conflictCount = responses.stream()
            .mapToInt(ExtractableResponse::statusCode)
            .filter(status -> status == CONFLICT.value())
            .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);
        assertThat(responses).hasSize(2);
    }

    @Test
    void 날짜가_null인_요청_시_예약_실패() {
        // when - 고객이 null 날짜로 예약을 시도하면
        Map<String, Object> invalidReservation = new ReservationTestDataBuilder()
            .withStartDate(null)
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(invalidReservation)
            .when()
            .post("/api/reservations")
            .then()
            .extract();

        // then - 예약에 실패한다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약 기간을 선택해주세요.");
    }

    @Test
    void 기존_예약을_완전히_포함하는_예약_실패() {
        // given - B-10 캠핑 구역에 12월 10일부터 13일까지 예약이 있을 때
        LocalDate existingStart = LocalDate.now().plusDays(10);
        LocalDate existingEnd = LocalDate.now().plusDays(13);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-10")
            .withDates(existingStart, existingEnd)
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(CREATED.value());

        // when - 동일한 캠핑 구역을 9일부터 14일까지 예약하려고 하면
        LocalDate newStart = LocalDate.now().plusDays(9);
        LocalDate newEnd = LocalDate.now().plusDays(14);

        Map<String, Object> newReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-10")
            .withDates(newStart, newEnd)
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(newReservation)
            .when()
                .post("/api/reservations")
            .then()
                .extract();

        // then - "해당 기간에 이미 예약이 존재합니다."라는 안내 메세지가 나타난다.
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 기존_예약_안에_포함되는_예약_실패() {
        // given - B-11 캠핑 구역에 12월 15일부터 20일까지 예약이 있을 때
        LocalDate existingStart = LocalDate.now().plusDays(15);
        LocalDate existingEnd = LocalDate.now().plusDays(20);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-11")
            .withDates(existingStart, existingEnd)
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(CREATED.value());

        // when - 동일한 캠핑 구역을 16일부터 18일까지 예약하려고 하면
        LocalDate newStart = LocalDate.now().plusDays(16);
        LocalDate newEnd = LocalDate.now().plusDays(18);

        Map<String, Object> newReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-11")
            .withDates(newStart, newEnd)
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(newReservation)
            .when()
                .post("/api/reservations")
            .then()
                .extract();

        // then - "해당 기간에 이미 예약이 존재합니다."라는 안내 메세지가 나타난다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 시작일만_겹치는_예약_실패() {
        // given - B-12 캠핑 구역에 20일부터 23일까지 예약이 있을 때
        LocalDate existingStart = LocalDate.now().plusDays(20);
        LocalDate existingEnd = LocalDate.now().plusDays(23);

        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-12")
            .withDates(existingStart, existingEnd)
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(CREATED.value());

        // when - 동일한 캠핑 구역을 22일부터 25일까지 예약하려고 하면
        LocalDate newStart = LocalDate.now().plusDays(22);
        LocalDate newEnd = LocalDate.now().plusDays(25);

        Map<String, Object> newReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-12")
            .withDates(newStart, newEnd)
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(newReservation)
            .when()
                .post("/api/reservations")
            .then()
                .extract();

        // then - "해당 기간에 이미 예약이 존재합니다."라는 안내 메세지가 나타난다.
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 종료일만_겹치는_예약_실패() {
        // given - B-13 캠핑 구역에 25일부터 28일까지 예약이 있을 때
        LocalDate existingStart = LocalDate.now().plusDays(25);
        LocalDate existingEnd = LocalDate.now().plusDays(28);

        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-13")
            .withDates(existingStart, existingEnd)
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(CREATED.value());

        // when - 동일한 캠핑 구역을 23일부터 26일까지 예약하려고 하면
        LocalDate newStart = LocalDate.now().plusDays(23);
        LocalDate newEnd = LocalDate.now().plusDays(26);

        Map<String, Object> newReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-13")
            .withDates(newStart, newEnd)
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(newReservation)
            .when()
                .post("/api/reservations")
            .then()
                .extract();

        // then - "해당 기간에 이미 예약이 존재합니다."라는 안내 메세지가 나타난다.
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 연속된_날짜_예약_성공() {
        // given - B-14 캠핑 구역에 12월 1일부터 3일까지 예약이 있을 때
        LocalDate existingStart = LocalDate.now().plusDays(1);
        LocalDate existingEnd = LocalDate.now().plusDays(3);

        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-14")
            .withDates(existingStart, existingEnd)
            .build();

        given()
            .contentType("application/json")
            .body(existingReservation)
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(CREATED.value());

        // when - 동일한 캠핑 구역을 4일부터 6일까지 예약하려고 하면
        LocalDate newStart = LocalDate.now().plusDays(4);
        LocalDate newEnd = LocalDate.now().plusDays(6);

        Map<String, Object> newReservation = new ReservationTestDataBuilder()
            .withSiteNumber("B-14")
            .withDates(newStart, newEnd)
            .build();

        ExtractableResponse<Response> response = given()
            .contentType("application/json")
            .body(newReservation)
            .when()
                .post("/api/reservations")
            .then()
                .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
    }
}
