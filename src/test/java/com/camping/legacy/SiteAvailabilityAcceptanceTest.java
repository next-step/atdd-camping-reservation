package com.camping.legacy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
class SiteAvailabilityAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 예약된_캠핑_구역은_예약_불가능으로_표시() {
        // given - A-3 캠핑 구역이 특정 날짜에 이미 예약되어 있을 때
        LocalDate reservedDate = LocalDate.now().plusDays(10);
        
        // 기존 예약 생성
        Map<String, Object> existingReservation = new HashMap<>();
        existingReservation.put("siteNumber", "A-3");
        existingReservation.put("startDate", reservedDate.toString());
        existingReservation.put("endDate", reservedDate.plusDays(1).toString());
        existingReservation.put("customerName", "예약된고객");
        existingReservation.put("phoneNumber", "010-1111-1111");
        
        given()
                .contentType("application/json")
                .body(existingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());
        
        // when - 고객이 해당 날짜에 A-3 캠핑 구역의 예약 가능 여부를 확인하면
        ExtractableResponse<Response> response = given()
        .when()
                .get("/api/sites/A-3/availability?date=" + reservedDate.toString())
        .then()
                .extract();
        
        // then - "예약 불가능"으로 표시된다
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-3");
    }

    @Test
    void 비어있는_캠핑_구역은_예약_가능으로_표시() {
        // given - A-4 캠핑 구역이 특정 날짜에 비어있을 때
        LocalDate availableDate = LocalDate.now().plusDays(15);
        
        // when - 고객이 해당 날짜에 A-4 캠핑 구역의 예약 가능 여부를 확인하면
        ExtractableResponse<Response> response = given()
        .when()
                .get("/api/sites/A-4/availability?date=" + availableDate.toString())
        .then()
                .extract();
        
        // then - "예약 가능"으로 표시된다
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-4");
    }

    @Test
    void 예약_완료_즉시_다른_고객에게는_예약_불가능으로_표시() {
        // given - A-6 캠핑 구역이 특정 날짜에 비어있을 때
        LocalDate targetDate = LocalDate.now().plusDays(12);
        
        // when - 고객A가 A-6 캠핑 구역을 해당 날짜에 예약한 직후
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("siteNumber", "A-6");
        reservation.put("startDate", targetDate.toString());
        reservation.put("endDate", targetDate.plusDays(1).toString());
        reservation.put("customerName", "고객A");
        reservation.put("phoneNumber", "010-2222-2222");
        
        given()
                .contentType("application/json")
                .body(reservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());
        
        // 고객B가 같은 날짜의 예약 가능 여부를 확인하면
        ExtractableResponse<Response> response = given()
        .when()
                .get("/api/sites/A-6/availability?date=" + targetDate.toString())
        .then()
                .extract();
        
        // then - "예약 불가능"으로 표시된다
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-6");
    }

    @Test
    void 여러_명이_동시에_확인해도_동일한_정보_제공() {
        // given - A-8 캠핑 구역이 특정 날짜에 예약되어 있을 때
        LocalDate checkDate = LocalDate.now().plusDays(14);
        
        // 기존 예약 생성
        Map<String, Object> existingReservation = new HashMap<>();
        existingReservation.put("siteNumber", "A-8");
        existingReservation.put("startDate", checkDate.toString());
        existingReservation.put("endDate", checkDate.plusDays(1).toString());
        existingReservation.put("customerName", "기존고객");
        existingReservation.put("phoneNumber", "010-3333-3333");
        
        given()
                .contentType("application/json")
                .body(existingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());
        
        // when - 10명의 고객이 동시에 A-8 캠핑 구역의 예약 가능 여부를 확인하면
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                try {
                    ExtractableResponse<Response> response = given()
                    .when()
                            .get("/api/sites/A-8/availability?date=" + checkDate.toString())
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
        
        // then - 모든 고객에게 동일하게 "예약 불가능"으로 표시된다
        assertThat(responses).hasSize(10);
        
        for (ExtractableResponse<Response> response : responses) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.jsonPath().getBoolean("available")).isFalse();
            assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-8");
        }
    }
}