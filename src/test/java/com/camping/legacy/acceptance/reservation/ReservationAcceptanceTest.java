package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTest;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceStep.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationAcceptanceTest extends AcceptanceTest {
    @DisplayName("예약 생성 - 성공")
    @Test
    void createReservation() {
        ReservationRequest request = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-1234-5678"
        );

        ReservationResponse response = 예약_생성_성공(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo(request.getCustomerName());
        assertThat(response.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
        assertThat(response.getSiteNumber()).isEqualTo(request.getSiteNumber());
        assertThat(response.getConfirmationCode()).isNotNull();
    }

    @DisplayName("예약 생성 - 30일 초과 예약 실패")
    @Test
    void createReservationFailWithOver30Days() {
        ReservationRequest request = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(31),
                LocalDate.now().plusDays(32),
                "A-1",
                "010-1234-5678"
        );

        var message = 예약_생성_실패(request, 409);

        assertThat(message).isEqualTo("예약일이 오늘 기준 30일을 초과할 수 없습니다.");
    }

    @DisplayName("예약 생성 - 과거 날짜 예약 실패")
    @Test
    void createReservationFailWithPastDate() {
        ReservationRequest request = getReservationRequest(
                "홍길동",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                "A-1",
                "010-1234-5678"
        );

        var message = 예약_생성_실패(request, 409);

        assertThat(message).contains("예약일이 과거일 수 없습니다.");
    }

    @DisplayName("예약 생성 - 종료일이 시작일 이전인 예약 실패")
    @Test
    void createReservationFailWithEndDateBeforeStartDate() {
        ReservationRequest request = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().minusDays(1),
                "A-1",
                "010-1234-5678"
        );

        var message = 예약_생성_실패(request, 409);
        
        assertThat(message).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @DisplayName("예약 생성 - 동일 사이트 동일 날짜 중복 예약 실패")
    @Test
    void createReservationFailWithDuplicateReservation() {
        ReservationRequest firstRequest = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-1234-5678"
        );
        예약_생성_성공(firstRequest);
        ReservationRequest duplicateRequest = getReservationRequest(
                "김철수",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-9876-5432"
        );

        var message = 예약_생성_실패(duplicateRequest, 409);

        assertThat(message).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @DisplayName("예약 취소 - 성공")
    @Test
    void cancelReservation() {
        ReservationRequest request = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-1234-5678"
        );
        ReservationResponse reservation = 예약_생성_성공(request);

        var message = 예약_취소_성공(reservation.getId(), reservation.getConfirmationCode());

        assertThat(message).isEqualTo("예약이 취소되었습니다.");
    }

    @DisplayName("예약 생성 - 예약 취소 후 동일 사이트 동일 날짜 예약 성공")
    @Test
    void cancelReservationAndCreateDuplicateReservation() {
        ReservationRequest firstRequest = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-1234-5678"
        );
        ReservationResponse response1 = 예약_생성_성공(firstRequest);
        ReservationRequest duplicateRequest = getReservationRequest(
                "김철수",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-9876-5432"
        );
        예약_취소_성공(response1.getId(), response1.getConfirmationCode());

        ReservationResponse response2 = 예약_생성_성공(duplicateRequest);

        assertThat(response2.getCustomerName()).isEqualTo(duplicateRequest.getCustomerName());
        assertThat(response2.getStartDate()).isEqualTo(duplicateRequest.getStartDate());
        assertThat(response2.getEndDate()).isEqualTo(duplicateRequest.getEndDate());
        assertThat(response2.getSiteNumber()).isEqualTo(duplicateRequest.getSiteNumber());
        assertThat(response2.getPhoneNumber()).isEqualTo(duplicateRequest.getPhoneNumber());
    }

    @DisplayName("예약 생성 - 동시성 테스트")
    @Test
    void createReservation_동시성_테스트() throws Exception {
        // Given
        ReservationRequest request = getReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-1234-5678"
        );

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 10개의 스레드가 동시에 같은 예약을 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 각 스레드마다 다른 고객 정보로 같은 사이트/날짜 예약 시도
                    ReservationRequest concurrentRequest = getReservationRequest(
                            "고객" + index,
                            request.getStartDate(),
                            request.getEndDate(),
                            request.getSiteNumber(),
                            "010-1234-567" + index
                    );
                    
                    예약_생성_성공(concurrentRequest);
                    successCount.incrementAndGet();
                } catch (AssertionError e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 스레드 완료 대기 (최대 10초)
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 동시성 제어가 제대로 되면 1개만 성공, 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
    }
}
