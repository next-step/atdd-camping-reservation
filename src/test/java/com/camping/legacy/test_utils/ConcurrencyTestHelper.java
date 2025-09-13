package com.camping.legacy.test_utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConcurrencyTestHelper {

    public static <T> ConcurrencyTestResult executeConcurrentTask(
            Supplier<T> taskDataSupplier,
            Function<T, Void> taskExecutor,
            int threadCount
    ) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> completableFutures = executeTasks(taskDataSupplier, taskExecutor, threadCount, successCount, failCount);
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

        return new ConcurrencyTestResult(successCount.get(), failCount.get());

    }

    private static <T> List<CompletableFuture<Void>> executeTasks(
            Supplier<T> taskDataSupplier,
            Function<T, Void> taskExecutor,
            int threadCount,
            AtomicInteger successCount,
            AtomicInteger failCount) {

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    T taskData = taskDataSupplier.get();
                    taskExecutor.apply(taskData);
                    successCount.incrementAndGet();
                } catch (Exception | AssertionError e) {
                    failCount.incrementAndGet();
                }
            });
            futures.add(future);
        }
        return futures;
    }

    public record ConcurrencyTestResult(int successCount, int failCount) {
    }
}
