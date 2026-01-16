package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_요청;
import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_목록_조회;
import com.camping.legacy.CampingApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 예약 처리 인수 테스트
 *
 * 인수 조건 P0-1:
 * "두 고객이 동시에 같은 사이트를 예약하면 정확히 1건만 성공해야 한다"
 */
@DisplayName("동시 예약 처리 인수 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = CampingApplication.class)
class ConcurrencyAcceptanceTest {

    @LocalServerPort
    private int port;

    private static final LocalDate BASE_DATE = LocalDate.now().plusDays(30);

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("두 고객이 동시에 같은 사이트를 예약하면 한 건만 성공한다")
    void 두_고객이_동시에_같은_사이트_예약시_한_건만_성공한다() throws InterruptedException {
        // Given
        LocalDate startDate = BASE_DATE;
        LocalDate endDate = BASE_DATE.plusDays(1);
        String siteNumber = "A-1";

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // When - 두 고객이 동시에 예약 요청
        Future<ExtractableResponse<Response>> future1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return 예약_요청(siteNumber, "김철수", "010-1111-1111", startDate, endDate);
        });

        Future<ExtractableResponse<Response>> future2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return 예약_요청(siteNumber, "이영희", "010-2222-2222", startDate, endDate);
        });

        readyLatch.await();
        startLatch.countDown(); // 동시 시작

        // Then
        ExtractableResponse<Response> response1 = getResponse(future1);
        ExtractableResponse<Response> response2 = getResponse(future2);

        long successCount = countSuccessResponses(response1, response2);

        assertThat(successCount)
                .as("정확히 1명만 예약에 성공해야 한다")
                .isEqualTo(1);

        // API로 예약 목록 조회하여 1건만 존재하는지 확인
        List<?> reservations = 예약_목록_조회(startDate);
        assertThat(reservations)
                .as("해당 기간의 예약 목록을 조회하면 1건만 존재한다")
                .hasSize(1);

        // 성공한 응답에는 6자리 확인 코드가 있어야 함
        ExtractableResponse<Response> successResponse = response1.statusCode() == 201 ? response1 : response2;
        String confirmationCode = successResponse.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode)
                .as("성공한 고객은 6자리 확인 코드를 받는다")
                .hasSize(6);

        executor.shutdown();
    }

    @Test
    @DisplayName("다섯 고객이 동시에 같은 사이트를 예약하면 한 건만 성공한다")
    void 다섯_고객이_동시에_같은_사이트_예약시_한_건만_성공한다() throws InterruptedException {
        // Given
        LocalDate startDate = BASE_DATE.plusDays(5);
        LocalDate endDate = startDate.plusDays(1);
        String siteNumber = "A-1";
        int numberOfCustomers = 5;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfCustomers);
        CountDownLatch readyLatch = new CountDownLatch(numberOfCustomers);
        CountDownLatch startLatch = new CountDownLatch(1);

        // When - 5명이 동시에 예약 요청
        List<Future<ExtractableResponse<Response>>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfCustomers; i++) {
            final int customerIndex = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return 예약_요청(siteNumber, "고객" + customerIndex, "010-000" + customerIndex + "-0000", startDate, endDate);
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // 동시 시작

        // Then
        List<ExtractableResponse<Response>> responses = new ArrayList<>();
        for (Future<ExtractableResponse<Response>> future : futures) {
            responses.add(getResponse(future));
        }

        long successCount = responses.stream()
                .filter(r -> r.statusCode() == HttpStatus.CREATED.value())
                .count();

        long failCount = responses.stream()
                .filter(r -> r.statusCode() != HttpStatus.CREATED.value())
                .count();

        assertThat(successCount)
                .as("정확히 1명만 예약에 성공해야 한다")
                .isEqualTo(1);

        assertThat(failCount)
                .as("4명은 예약에 실패해야 한다")
                .isEqualTo(4);

        // API로 예약 목록 조회
        List<?> reservations = 예약_목록_조회(startDate);
        assertThat(reservations)
                .as("해당 기간의 예약 목록을 조회하면 1건만 존재한다")
                .hasSize(1);

        executor.shutdown();
    }

    @Test
    @DisplayName("서로 다른 사이트에 동시 예약하면 모두 성공한다")
    void 서로_다른_사이트에_동시_예약하면_모두_성공한다() throws InterruptedException {
        // Given
        LocalDate startDate = BASE_DATE.plusDays(10);
        LocalDate endDate = startDate.plusDays(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // When - 서로 다른 사이트에 동시 예약
        Future<ExtractableResponse<Response>> future1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return 예약_요청("A-1", "김철수", "010-1111-1111", startDate, endDate);
        });

        Future<ExtractableResponse<Response>> future2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return 예약_요청("A-2", "이영희", "010-2222-2222", startDate, endDate);
        });

        readyLatch.await();
        startLatch.countDown(); // 동시 시작

        // Then
        ExtractableResponse<Response> response1 = getResponse(future1);
        ExtractableResponse<Response> response2 = getResponse(future2);

        assertThat(response1.statusCode())
                .as("김철수의 A-1 예약이 성공해야 한다")
                .isEqualTo(HttpStatus.CREATED.value());

        assertThat(response2.statusCode())
                .as("이영희의 A-2 예약이 성공해야 한다")
                .isEqualTo(HttpStatus.CREATED.value());

        executor.shutdown();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 동시성 테스트 전용 유틸리티 메서드
    // ═══════════════════════════════════════════════════════════════════════════

    private ExtractableResponse<Response> getResponse(Future<ExtractableResponse<Response>> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long countSuccessResponses(ExtractableResponse<Response>... responses) {
        long count = 0;
        for (ExtractableResponse<Response> response : responses) {
            if (response.statusCode() == HttpStatus.CREATED.value()) {
                count++;
            }
        }
        return count;
    }
}
