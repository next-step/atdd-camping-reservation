package com.camping.legacy.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.CONFLICT;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConcurrentTestHelper {

    public static <T> List<T> runConcurrently(int threadCount, Supplier<T> task) {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<T> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    T result = task.get();
                    synchronized (results) {
                        results.add(result);
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

        return results;
    }

    public static void assertConcurrentResults(List<ExtractableResponse<Response>> responses, 
                                             int expectedSuccessCount, int expectedConflictCount) {
        long successCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == CREATED.value())
                .count();

        long conflictCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == CONFLICT.value())
                .count();

        assertThat(successCount).isEqualTo(expectedSuccessCount);
        assertThat(conflictCount).isEqualTo(expectedConflictCount);
        assertThat(responses).hasSize(expectedSuccessCount + expectedConflictCount);
    }

    public static List<ExtractableResponse<Response>> executeConcurrentReservations(
            int threadCount, Supplier<Map<String, Object>> reservationDataSupplier) {
        return runConcurrently(threadCount, () -> 
            ReservationApiHelper.createReservation(reservationDataSupplier.get())
        );
    }

    public static List<ExtractableResponse<Response>> executeConcurrentReservationsWithIndex(
            int threadCount, Function<Integer, Map<String, Object>> reservationDataFunction) {
        return runConcurrently(threadCount, () -> {
            int customerIndex = (int) (Thread.currentThread().getId() % threadCount);
            return ReservationApiHelper.createReservation(reservationDataFunction.apply(customerIndex));
        });
    }

    public static List<ExtractableResponse<Response>> executeConcurrentSiteAvailabilityChecks(
            int threadCount, String siteNumber, LocalDate date) {
        return runConcurrently(threadCount, () -> 
            ReservationApiHelper.checkSiteAvailability(siteNumber, date)
        );
    }
}