package com.camping.legacy.integration;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 수정 동시성 테스트")
class ReservationModifyConcurrencyTest extends AcceptanceTest {

    @Test
    @DisplayName("동시에 같은 기간으로 예약 수정 시 하나만 성공")
    void 동시_예약_수정_하나만_성공() throws InterruptedException {
        // given - 같은 사이트에 서로 다른 기간의 예약 2개 생성
        ExtractableResponse<Response> 예약1 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(200), 오늘부터_N일_후(202), "홍길동", 4
        );
        ExtractableResponse<Response> 예약2 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(210), 오늘부터_N일_후(212), "김철수", 4
        );

        Long 예약1_id = 예약1.jsonPath().getLong("id");
        Long 예약2_id = 예약2.jsonPath().getLong("id");
        String 확인코드1 = 예약1.jsonPath().getString("confirmationCode");
        String 확인코드2 = 예약2.jsonPath().getString("confirmationCode");

        // 충돌할 목표 기간
        String 목표_시작일 = 오늘부터_N일_후(220);
        String 목표_종료일 = 오늘부터_N일_후(222);

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Integer> statusCodes = new ArrayList<>();

        // when - 동시에 같은 기간으로 수정 시도
        executorService.submit(() -> {
            try {
                ExtractableResponse<Response> response = 예약_수정_요청_without_log(
                        예약1_id, 확인코드1, 목표_시작일, 목표_종료일
                );
                synchronized (statusCodes) {
                    statusCodes.add(response.statusCode());
                }
                if (response.statusCode() == HttpStatus.OK.value()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                ExtractableResponse<Response> response = 예약_수정_요청_without_log(
                        예약2_id, 확인코드2, 목표_시작일, 목표_종료일
                );
                synchronized (statusCodes) {
                    statusCodes.add(response.statusCode());
                }
                if (response.statusCode() == HttpStatus.OK.value()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        // then - 하나만 성공하고 하나는 실패해야 함
        assertThat(successCount.get())
                .as("동시 수정 시 하나만 성공해야 합니다")
                .isEqualTo(1);
        assertThat(failCount.get())
                .as("동시 수정 시 하나는 실패해야 합니다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("여러 스레드가 동시에 같은 기간으로 예약 수정 시 하나만 성공")
    void 다중_스레드_동시_예약_수정() throws InterruptedException {
        // given - 같은 사이트에 서로 다른 기간의 예약 5개 생성
        int reservationCount = 5;
        List<Long> reservationIds = new ArrayList<>();
        List<String> confirmationCodes = new ArrayList<>();

        for (int i = 0; i < reservationCount; i++) {
            ExtractableResponse<Response> 예약 = 예약_생성_요청(
                    "A-1",
                    오늘부터_N일_후(300 + i * 10),
                    오늘부터_N일_후(302 + i * 10),
                    "고객" + i,
                    4
            );
            reservationIds.add(예약.jsonPath().getLong("id"));
            confirmationCodes.add(예약.jsonPath().getString("confirmationCode"));
        }

        // 충돌할 목표 기간
        String 목표_시작일 = 오늘부터_N일_후(400);
        String 목표_종료일 = 오늘부터_N일_후(402);

        ExecutorService executorService = Executors.newFixedThreadPool(reservationCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(reservationCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 같은 기간으로 수정 시도
        for (int i = 0; i < reservationCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기
                    ExtractableResponse<Response> response = 예약_수정_요청_without_log(
                            reservationIds.get(index),
                            confirmationCodes.get(index),
                            목표_시작일,
                            목표_종료일
                    );
                    if (response.statusCode() == HttpStatus.OK.value()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 시작
        endLatch.await();
        executorService.shutdown();

        // then - 하나만 성공하고 나머지는 실패해야 함
        assertThat(successCount.get())
                .as("동시 수정 시 하나만 성공해야 합니다")
                .isEqualTo(1);
        assertThat(failCount.get())
                .as("동시 수정 시 나머지는 실패해야 합니다")
                .isEqualTo(reservationCount - 1);
    }

    private ExtractableResponse<Response> 예약_수정_요청_without_log(
            Long reservationId, String confirmationCode,
            String startDate, String endDate) {

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        return RestAssured
                .given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("confirmationCode", confirmationCode)
                .body(params)
                .when().put("/api/reservations/{id}", reservationId)
                .then()
                .extract();
    }
}