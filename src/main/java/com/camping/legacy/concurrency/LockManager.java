package com.camping.legacy.concurrency;

public interface LockManager {

    <T> T lock(String lockKey, Process<T> process);

    interface Process<T> {

        T proceed();
    }
}
