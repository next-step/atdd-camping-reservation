package com.camping.legacy.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrencyTestHelper {
    public static void execute(Runnable... tasks) throws InterruptedException {
        int size = tasks.length;
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        CountDownLatch readyLatch = new CountDownLatch(size);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(size);

        for (Runnable task : tasks) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();     // 모든 스레드가 준비될 때까지 대기
        startLatch.countDown(); // "빵!" 하고 동시에 시작

        doneLatch.await();      // 모든 작업 완료 대기
        executorService.shutdown();
    }
}
