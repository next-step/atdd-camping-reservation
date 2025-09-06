package com.camping.legacy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConcurrencyTestHelper {

    public static <T> ConcurrencyTestResult executeConcurrentTasks(
            Supplier<T> taskDataSupplier,
            Function<T, Void> taskExecutor,
            int threadCount) {
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = createConcurrentTasks(
                taskDataSupplier, taskExecutor, threadCount, successCount, failureCount
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new ConcurrencyTestResult(successCount.get(), failureCount.get());
    }

    private static <T> List<CompletableFuture<Void>> createConcurrentTasks(
            Supplier<T> taskDataSupplier,
            Function<T, Void> taskExecutor,
            int threadCount,
            AtomicInteger successCount,
            AtomicInteger failureCount) {
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                T taskData = taskDataSupplier.get();
                executeTask(taskExecutor, taskData, successCount, failureCount);
            });
            futures.add(future);
        }

        return futures;
    }

    private static <T> void executeTask(
            Function<T, Void> taskExecutor,
            T taskData,
            AtomicInteger successCount,
            AtomicInteger failureCount) {
        try {
            taskExecutor.apply(taskData);
            successCount.incrementAndGet();
        } catch (Exception | AssertionError e) {
            failureCount.incrementAndGet();
        }
    }

    public record ConcurrencyTestResult(int successCount, int failureCount) {

    }
}
