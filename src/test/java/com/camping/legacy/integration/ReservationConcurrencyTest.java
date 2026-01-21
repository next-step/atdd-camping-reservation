package com.camping.legacy.integration;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 동시성 테스트")
class ReservationConcurrencyTest extends AcceptanceTest {

    @Test
    @DisplayName("동시 예약 요청 시 하나만 성공")
    void 동시_예약_하나만_성공() throws InterruptedException {
        // given
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        String startDate = 오늘부터_N일_후(40);
        String endDate = 오늘부터_N일_후(45);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when
        Runnable task = () -> {
            try {
                startLatch.await();
                ExtractableResponse<Response> response = 예약_생성_요청(
                        "A-1", startDate, endDate, "고객", 4
                );
                if (response.statusCode() == HttpStatus.CREATED.value()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task);
        executor.submit(task);
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).as("하나만 성공").isEqualTo(1);
        assertThat(failCount.get()).as("하나는 실패").isEqualTo(1);
    }
}