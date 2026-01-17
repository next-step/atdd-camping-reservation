package com.camping.legacy.util.aop;

import jakarta.persistence.OptimisticLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP (Aspect-Oriented Programming, 관점 지향 프로그래밍)를 사용하여
 * Optimistic Lock 충돌 시 재시도 로직을 처리하는 Aspect 클래스.
 *
 * @Aspect: 이 클래스가 AOP의 Aspect임을 나타냅니다. Aspect는 여러 비즈니스 로직에 공통으로 적용될 부가 기능(Advice)과
 *          적용될 지점(Pointcut)을 합친 모듈입니다. 여기서는 '재시도 로직'이 부가 기능에 해당합니다.
 * @Component: 이 클래스를 Spring의 Bean으로 등록하여, Spring 컨테이너가 관리하도록 합니다.
 */
@Aspect
@Component
public class OptimisticLockRetryAspect {

    // 재시도 횟수
    private static final int MAX_RETRIES = 3;
    // 재시도 사이의 대기 시간 (50ms)
    private static final long RETRY_DELAY_MS = 50;

    // 로그 출력을 위한 Logger 객체
    private static final Logger log = LoggerFactory.getLogger(OptimisticLockRetryAspect.class);

    @Around("execution(* com.camping.legacy..*.*(..)) && @annotation(retryOnOptimisticLock)")
    public Object retryOnOptimisticLock(ProceedingJoinPoint joinPoint, RetryOnOptimisticLock retryOnOptimisticLock) throws Throwable {
        int retryCount = 0;
        Exception lastException = null;

        // MAX_RETRIES 횟수만큼 재시도 루프를 돕니다.
        while (retryCount < MAX_RETRIES) {
            try {
                return joinPoint.proceed();

            } catch (OptimisticLockException e) {
                lastException = e;
                retryCount++;
                log.warn("Optimistic lock 충돌 발생. 재시도합니다. (시도: {}/{}) - 대상: {}", retryCount, MAX_RETRIES, joinPoint.getSignature().toShortString());

                // 마지막 시도가 아니라면, 잠시 대기 후 다시 시도합니다.
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // 대기 중 인터럽트가 발생하면, 재시도를 중단하고 예외를 던집니다.
                        throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
                    }
                }
            }
        }
        throw lastException;
    }
}
