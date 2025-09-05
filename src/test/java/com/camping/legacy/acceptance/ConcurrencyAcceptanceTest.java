package com.camping.legacy.acceptance;

import static com.camping.legacy.acceptance.helper.ReservationTestHelper.createReservation;
import static com.camping.legacy.acceptance.helper.ReservationTestHelper.reservationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ConcurrencyAcceptanceTest extends AcceptanceTestBase {

    private static final int NUMBER_OF_THREADS = 3;

    @DisplayName("동일 사이트, 동일 날짜에 3개의 예약이 요청될 때 1개의 예약만 성공해야 한다.")
    @Test
    void concurrentReservationsTest() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        // when
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            executorService.submit(() -> {
                try {
                    int statusCode = createReservation(reservationRequest().build()).statusCode();

                    statusCodes.add(statusCode);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        long successCount = statusCodes.stream().filter(status -> status == HttpStatus.CREATED.value()).count();
        long failCount = statusCodes.stream().filter(status -> status == HttpStatus.CONFLICT.value()).count();

        assertAll(
                () -> assertThat(successCount).isEqualTo(1),
                () -> assertThat(failCount).isEqualTo(NUMBER_OF_THREADS - 1)
        );
    }
}
