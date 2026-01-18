package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.fixture.ReservationFixture;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("동시성 제어 인수 테스트")
class ConcurrencyAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        CampsiteFixture.createDefaultSites(campsiteRepository);
    }

    @Nested
    @DisplayName("동시 예약 요청")
    class ConcurrentReservationRequests {

        @Test
        @Disabled("ISSUE-003: 동시성 제어 로직 구현 필요 - 현재는 여러 건이 성공할 수 있음")
        @DisplayName("동일 사이트에 동시 예약 요청 시 하나만 성공한다")
        void shouldAllowOnlyOneReservationForSameSite() throws InterruptedException {
            // given
            LocalDate targetDate = LocalDate.now().plusDays(7);
            int numberOfConcurrentRequests = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfConcurrentRequests);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfConcurrentRequests);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            List<Future<ExtractableResponse<Response>>> futures = new ArrayList<>();

            // when
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                final int index = i;
                Future<ExtractableResponse<Response>> future = executorService.submit(() -> {
                    try {
                        startLatch.await();
                        ReservationRequest request = ReservationFixture.createConcurrencyRequest(
                                index, "A-1", targetDate, targetDate.plusDays(2));
                        return RestAssured.given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/api/reservations")
                                .then()
                                .extract();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown();
            doneLatch.await(30, TimeUnit.SECONDS);

            // then
            for (Future<ExtractableResponse<Response>> future : futures) {
                try {
                    ExtractableResponse<Response> response = future.get();
                    if (response.statusCode() == HttpStatus.CREATED.value()) {
                        successCount.incrementAndGet();
                    } else if (response.statusCode() == HttpStatus.CONFLICT.value()) {
                        failCount.incrementAndGet();
                    }
                } catch (ExecutionException e) {
                    failCount.incrementAndGet();
                }
            }

            executorService.shutdown();

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failCount.get()).isEqualTo(numberOfConcurrentRequests - 1);
        }

        @Test
        @DisplayName("서로 다른 사이트에 동시 예약 요청 시 모두 성공한다")
        void shouldAllowAllReservationsForDifferentSites() throws InterruptedException {
            // given
            LocalDate targetDate = LocalDate.now().plusDays(7);
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);

            List<Future<ExtractableResponse<Response>>> futures = new ArrayList<>();

            String[] sites = {"A-1", "A-2"};
            String[] customers = {"홍길동", "김철수"};

            // when
            for (int i = 0; i < 2; i++) {
                final int index = i;
                Future<ExtractableResponse<Response>> future = executorService.submit(() -> {
                    try {
                        startLatch.await();
                        ReservationRequest request = ReservationFixture.createRequest(
                                customers[index], sites[index], targetDate, targetDate.plusDays(2));
                        return RestAssured.given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/api/reservations")
                                .then()
                                .extract();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown();
            doneLatch.await(30, TimeUnit.SECONDS);

            // then
            int successCount = 0;
            for (Future<ExtractableResponse<Response>> future : futures) {
                try {
                    ExtractableResponse<Response> response = future.get();
                    if (response.statusCode() == HttpStatus.CREATED.value()) {
                        successCount++;
                    }
                } catch (ExecutionException e) {
                    // ignore
                }
            }

            executorService.shutdown();

            assertThat(successCount).isEqualTo(2);
        }
    }
}
