package com.camping.legacy.acceptance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static com.camping.legacy.acceptance.ReservationSteps.createReservation;
import static com.camping.legacy.acceptance.ReservationSteps.createReservationExpectingFailure;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationCreateAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("유효한 정보로 예약 생성 시 201 응답과 확인코드 발급")
    void 예약_생성_성공() {
        // When - 예약 생성 요청
        var response = createReservation("홍길동", "A-1", 1, 3);

        // Then - 응답 검증
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
    }

    @Test
    @DisplayName("존재하지 않는 사이트로 예약 시 실패")
    void 존재하지_않는_사이트_예약_실패() {
        // When - 존재하지 않는 사이트로 예약 요청
        var response = createReservationExpectingFailure("홍길동", "Z99", 1, 3);

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("과거 날짜로 예약 시 실패")
    void 과거_날짜_예약_실패() {
        // When - 과거 날짜로 예약 요청
        var response = createReservationExpectingFailure("홍길동", "A-1", -1, 1);

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("동일 사이트 겹치는 기간 예약 시 실패")
    void 중복_예약_실패() {
        // Given - 첫 번째 예약 생성
        createReservation("홍길동", "A-1", 10, 15);

        // When - 겹치는 기간으로 두 번째 예약 시도
        var response = createReservationExpectingFailure("김철수", "A-1", 12, 17);

        // Then - 중복 예약 실패 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전이면 실패")
    void 종료일이_시작일_이전_실패() {
        // When - 종료일 < 시작일로 예약 요청
        var response = createReservationExpectingFailure("김철수", "A-1", 5, 3);

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("30일 초과 기간 예약 시 실패")
    void 최대_예약기간_초과_실패() {
        // When - 31일 이상 기간으로 예약 요청
        var response = createReservationExpectingFailure("김철수", "A-1", 1, 35);

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    /**
     * 레거시 버그 BUG-003: 동시 예약 요청 Race Condition
     * - 현재 상태: 둘 다 201 반환 가능 (버그)
     * - 기대 동작: 하나만 201, 다른 하나는 409
     */
    @Disabled("버그 수정 대상 - 동시성 Race Condition")
    @Test
    @DisplayName("동시에 같은 기간 예약 시 하나만 성공")
    void 동시_예약_요청_중복_방지() throws Exception {
        // Given
        var executor = Executors.newFixedThreadPool(2);
        var latch = new CountDownLatch(2);
        var results = new CopyOnWriteArrayList<Integer>();

        // When - 동시에 2개 요청
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            executor.submit(() -> {
                var response = createReservationExpectingFailure("고객" + idx, "A-1", 10, 15);
                results.add(response.statusCode());
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        // Then - 하나는 201, 하나는 409
        assertThat(results).containsExactly(201, 409);
    }

}
