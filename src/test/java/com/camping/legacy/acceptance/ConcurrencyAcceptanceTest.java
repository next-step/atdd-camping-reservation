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
            var targetDate = LocalDate.now().plusDays(7);
            var numberOfConcurrentRequests = 10;
            var executorService = Executors.newFixedThreadPool(numberOfConcurrentRequests);
            var startLatch = new CountDownLatch(1);
            var doneLatch = new CountDownLatch(numberOfConcurrentRequests);

            var successCount = new AtomicInteger(0);
            var failCount = new AtomicInteger(0);
            var futures = new ArrayList<Future<ExtractableResponse<Response>>>();

            // when
            for (var i = 0; i < numberOfConcurrentRequests; i++) {
                final var index = i;
                var future = executorService.submit(() -> {
                    try {
                        startLatch.await();
                        var request = ReservationFixture.createConcurrencyRequest(
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
            for (var future : futures) {
                try {
                    var response = future.get();
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
            var targetDate = LocalDate.now().plusDays(7);
            var executorService = Executors.newFixedThreadPool(2);
            var startLatch = new CountDownLatch(1);
            var doneLatch = new CountDownLatch(2);

            var futures = new ArrayList<Future<ExtractableResponse<Response>>>();

            var sites = new String[]{"A-1", "A-2"};
            var customers = new String[]{"홍길동", "김철수"};

            // when
            for (var i = 0; i < 2; i++) {
                final var index = i;
                var future = executorService.submit(() -> {
                    try {
                        startLatch.await();
                        var request = ReservationFixture.createRequest(
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
            var successCount = 0;
            for (var future : futures) {
                try {
                    var response = future.get();
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
