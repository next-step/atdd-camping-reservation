package com.camping.legacy.test.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class ConcurrencyUtils {

    public static void runSimultaneously(int concurrencySize, Block block) {
        var countDown = new CountDownLatch(concurrencySize);

        for (int i = 0; i < concurrencySize; i++) {
            CompletableFuture.runAsync(() -> {
                block.invoke();
                countDown.countDown();
            });
        }

        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface Block {

        void invoke();
    }
}
