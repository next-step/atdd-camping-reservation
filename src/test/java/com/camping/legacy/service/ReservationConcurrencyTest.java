package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ReservationService 동시성 테스트")
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final LocalDate NOW = LocalDate.now();

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
        campsiteRepository.save(new Campsite("A-1", "대형 사이트", 8));
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 사이트/기간에 동시 예약 시 하나만 성공해야 한다")
    void onlyOneReservationSucceedsWhenConcurrent() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        LocalDate startDate = NOW.plusDays(5);
        LocalDate endDate = NOW.plusDays(7);

        for (int i = 0; i < threadCount; i++) {
            final String name = "고객" + i;
            final String phone = "010-0000-" + String.format("%04d", i);
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    ReservationRequest request = new ReservationRequest(
                            name, startDate, endDate,
                            "A-1", phone,
                            null, null, null
                    );
                    reservationService.createReservation(request, NOW);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(successCount.get())
                .describedAs("동시 예약 시 정확히 1건만 성공해야 한다 (성공: %d, 실패: %d)",
                        successCount.get(), failCount.get())
                .isEqualTo(1);
    }
}
