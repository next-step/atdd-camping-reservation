package com.camping.legacy.concurrency;

import org.springframework.stereotype.Component;

@Component
public class LocalLockManager implements LockManager {

    @Override
    synchronized public <T> T lock(String lockKey, Process<T> process) {
        return process.proceed();
    }
}
