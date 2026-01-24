package com.camping.legacy.service;

import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.fixture.ReservationFixture;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("예약 서비스 동시성 테스트")
class ReservationServiceConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
        CampsiteFixture.createDefaultSites(campsiteRepository);
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
    }

    @Nested
    @DisplayName("동시 예약 요청")
    class ConcurrentReservationRequests {

        @Test
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

            // when
            for (var i = 0; i < numberOfConcurrentRequests; i++) {
                final var index = i;
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        var request = ReservationFixture.createConcurrencyRequest(index, "A-1", targetDate, targetDate.plusDays(2));
                        reservationService.createReservation(request);
                        successCount.incrementAndGet();
                    } catch (RuntimeException e) {
                        failCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(30, TimeUnit.SECONDS);
            shutdownExecutor(executorService);

            // then
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

            var successCount = new AtomicInteger(0);
            var sites = new String[]{"A-1", "A-2"};

            // when
            for (var i = 0; i < 2; i++) {
                final var index = i;
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        var request = ReservationFixture.createConcurrencyRequest(index, sites[index], targetDate, targetDate.plusDays(2));
                        reservationService.createReservation(request);
                        successCount.incrementAndGet();
                    } catch (RuntimeException e) {
                        // 실패
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(30, TimeUnit.SECONDS);
            shutdownExecutor(executorService);

            // then
            assertThat(successCount.get()).isEqualTo(2);
        }
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
