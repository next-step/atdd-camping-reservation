package com.camping.legacy.domain;

import com.camping.legacy.concurrency.LockManager;
import com.camping.legacy.concurrency.LockManager.Process;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ReservationLockManager {

    private final LockManager lockManager;

    public <T> T lock(String siteNumber, Process<T> process) {
        String lockKey = generateLockKey(siteNumber);
        return lockManager.lock(lockKey, process);
    }

    private String generateLockKey(String siteNumber) {
        return "reservation:site:%s".formatted(siteNumber);
    }
}
