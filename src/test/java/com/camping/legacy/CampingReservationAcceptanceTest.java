package com.camping.legacy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
class CampingReservationAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 예약_생성_성공_시_예약_번호_받기() {
        // given - A-2 캠핑 구역이 12월 25일부터 27일까지 비어있을 때
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = LocalDate.now().plusDays(9);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("siteNumber", "A-2");
        reservationRequest.put("startDate", startDate.toString());
        reservationRequest.put("endDate", endDate.toString());
        reservationRequest.put("customerName", "김테스트");
        reservationRequest.put("phoneNumber", "010-1234-5678");
        
        // when - 고객이 A-2 캠핑 구역을 예약하면
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
        // given - A-1 캠핑 구역이 12월 25일부터 27일까지 비어있을 때
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = LocalDate.now().plusDays(9);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("siteNumber", "A-1");
        reservationRequest.put("startDate", startDate.toString());
        reservationRequest.put("endDate", endDate.toString());
        reservationRequest.put("customerName", "동시테스트");
        reservationRequest.put("phoneNumber", "010-1111-1111");
        
        // when - 10명의 고객이 동시에 같은 기간으로 A-1 캠핑 구역을 예약하려고 하면
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            final int customerIndex = i;
            executorService.execute(() -> {
                try {
                    Map<String, Object> request = new HashMap<>(reservationRequest);
                    request.put("customerName", "동시테스트" + customerIndex);
                    request.put("phoneNumber", "010-1111-111" + customerIndex);
                    
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
                .filter(status -> status == 409)
                .count();
        
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(9);
        assertThat(responses).hasSize(10);
    }

    @Test
    void 이미_예약된_캠핑_구역은_예약_불가() {
        // given - B-1 캠핑 구역이 12월 25일부터 27일까지 이미 예약되어 있을 때
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        
        // 사전 예약 생성
        Map<String, Object> existingReservation = new HashMap<>();
        existingReservation.put("siteNumber", "B-1");
        existingReservation.put("startDate", startDate.toString());
        existingReservation.put("endDate", endDate.toString());
        existingReservation.put("customerName", "기존고객");
        existingReservation.put("phoneNumber", "010-9999-9999");
        
        given()
                .contentType("application/json")
                .body(existingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());
        
        // 동일한 기간으로 새로운 예약 시도
        Map<String, Object> newReservation = new HashMap<>();
        newReservation.put("siteNumber", "B-1");
        newReservation.put("startDate", startDate.toString());
        newReservation.put("endDate", endDate.toString());
        newReservation.put("customerName", "새로운고객");
        newReservation.put("phoneNumber", "010-8888-8888");
        
        // when - 고객이 같은 기간으로 B-1 캠핑 구역을 예약하려고 하면
        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(newReservation)
        .when()
                .post("/api/reservations")
        .then()
                .extract();
        
        // then - "해당 기간에 이미 예약이 존재합니다"라는 안내 메시지가 나타난다
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }
}
