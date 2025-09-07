package com.camping.legacy.acceptance;

import static com.camping.legacy.acceptance.helper.ReservationTestHelper.createReservation;
import static com.camping.legacy.acceptance.helper.ReservationTestHelper.reservationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @DisplayName("부분 겹침 동시 예약 요청 시 하나의 예약만 성공한다.")
    @Test
    void partialOverlapConcurrentReservationsTest() throws InterruptedException {
        // given
        LocalDate kimStartDate = TODAY.plusDays(3);  // 2025-09-10
        LocalDate kimEndDate = kimStartDate.plusDays(2); // 2025-09-12

        LocalDate leeStartDate = kimStartDate.plusDays(1); // 2025-09-11
        LocalDate leeEndDate = leeStartDate.plusDays(2);   // 2025-09-13

        Map<String, String> kimRequest = reservationRequest()
                .withCustomerName("김철수")
                .withPhoneNumber("010-1111-1111")
                .withStartDate(kimStartDate.toString())
                .withEndDate(kimEndDate.toString())
                .withSiteNumber("A-1")
                .build();

        Map<String, String> leeRequest = reservationRequest()
                .withCustomerName("이영희")
                .withPhoneNumber("010-2222-2222")
                .withStartDate(leeStartDate.toString())
                .withEndDate(leeEndDate.toString())
                .withSiteNumber("A-1")
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch latch = new CountDownLatch(2);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();
        List<String> errorMessages = new CopyOnWriteArrayList<>();

        // when
        executorService.submit(() -> {
            try {
                ExtractableResponse<Response> response = createReservation(kimRequest);
                statusCodes.add(response.statusCode());
                if (response.statusCode() != HttpStatus.CREATED.value()) {
                    errorMessages.add(response.jsonPath().getString("message"));
                }
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                ExtractableResponse<Response> response = createReservation(leeRequest);
                statusCodes.add(response.statusCode());
                if (response.statusCode() != HttpStatus.CREATED.value()) {
                    errorMessages.add(response.jsonPath().getString("message"));
                }
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        // then - 하나의 예약만 성공하고, 다른 예약은 중복 메시지와 함께 실패
        long successCount = statusCodes.stream().filter(status -> status == HttpStatus.CREATED.value()).count();
        long failCount = statusCodes.stream().filter(status -> status == HttpStatus.CONFLICT.value()).count();

        assertAll(
                () -> assertThat(successCount).isEqualTo(1),
                () -> assertThat(failCount).isEqualTo(1),
                () -> assertThat(errorMessages).hasSize(1),
                () -> assertThat(errorMessages.get(0)).isEqualTo("해당 기간에 이미 예약이 존재합니다.")
        );
    }

}
