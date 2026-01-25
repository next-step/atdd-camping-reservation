package com.camping.legacy.service;

import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.builder.ReservationRequestBuilder;
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

import static com.camping.legacy.builder.ReservationRequestBuilder.aReservation;
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
    void 사전_데이터_준비() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
        CampsiteFixture.기본_사이트_생성(campsiteRepository);
    }

    @AfterEach
    void 데이터_정리() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
    }

    @Nested
    @DisplayName("동시 예약 요청")
    class 동시_예약_요청 {

        @Test
        @DisplayName("동일 사이트에 동시 예약 요청 시 하나만 성공한다")
        void 동일_사이트에_동시_예약_요청시_하나만_성공한다() throws InterruptedException {
            // given
            var 예약날짜 = LocalDate.now().plusDays(7);
            var 동시요청수 = 10;
            var executorService = Executors.newFixedThreadPool(동시요청수);
            var 시작신호 = new CountDownLatch(1);
            var 완료신호 = new CountDownLatch(동시요청수);

            var 성공횟수 = new AtomicInteger(0);
            var 실패횟수 = new AtomicInteger(0);

            // when
            for (var i = 0; i < 동시요청수; i++) {
                final var 인덱스 = i;
                executorService.submit(() -> {
                    try {
                        시작신호.await();
                        var 요청 = aReservation()
                                .siteNumber("A-1")
                                .period(예약날짜, 예약날짜.plusDays(2))
                                .forConcurrencyTest(인덱스)
                                .build();
                        reservationService.createReservation(요청);
                        성공횟수.incrementAndGet();
                    } catch (RuntimeException e) {
                        실패횟수.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        완료신호.countDown();
                    }
                });
            }

            시작신호.countDown();
            완료신호.await(30, TimeUnit.SECONDS);
            executor_종료(executorService);

            // then
            assertThat(성공횟수.get()).isEqualTo(1);
            assertThat(실패횟수.get()).isEqualTo(동시요청수 - 1);
        }

        @Test
        @DisplayName("서로 다른 사이트에 동시 예약 요청 시 모두 성공한다")
        void 서로_다른_사이트에_동시_예약_요청시_모두_성공한다() throws InterruptedException {
            // given
            var 예약날짜 = LocalDate.now().plusDays(7);
            var executorService = Executors.newFixedThreadPool(2);
            var 시작신호 = new CountDownLatch(1);
            var 완료신호 = new CountDownLatch(2);

            var 성공횟수 = new AtomicInteger(0);
            var 사이트목록 = new String[]{"A-1", "A-2"};

            // when
            for (var i = 0; i < 2; i++) {
                final var 인덱스 = i;
                executorService.submit(() -> {
                    try {
                        시작신호.await();
                        var 요청 = aReservation()
                                .siteNumber(사이트목록[인덱스])
                                .period(예약날짜, 예약날짜.plusDays(2))
                                .forConcurrencyTest(인덱스)
                                .build();
                        reservationService.createReservation(요청);
                        성공횟수.incrementAndGet();
                    } catch (RuntimeException e) {
                        // 실패
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        완료신호.countDown();
                    }
                });
            }

            시작신호.countDown();
            완료신호.await(30, TimeUnit.SECONDS);
            executor_종료(executorService);

            // then
            assertThat(성공횟수.get()).isEqualTo(2);
        }
    }

    private void executor_종료(ExecutorService executorService) {
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
