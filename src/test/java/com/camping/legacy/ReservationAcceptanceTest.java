package com.camping.legacy;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: 중복 예약 방지
 *
 * @see docs/4.scenarios.feature - "중복 예약 방지" Feature 참조
 */
@DisplayName("예약 생성 인수 테스트")
class ReservationAcceptanceTest extends AcceptanceTest {

    @BeforeEach
    void setUpFixture() {
        // Background: 사이트 "A-1"이 등록되어 있다 (DatabaseCleanup에서 기본 데이터 복원됨)
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyPath {

        @Test
        @DisplayName("[정상] 예약이 없는 기간에 정상적으로 예약 생성")
        void 예약_생성_성공() {
            // given - "A-1" 사이트의 해당 기간에 예약이 없다

            // when - "김철수"가 예약을 요청한다
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "A-1", "2026-01-01", "2026-01-18", "김철수", 4
            );

            // then - 예약이 성공적으로 생성된다
            예약_생성_성공_검증(response, "김철수");
        }

        @Test
        @DisplayName("[정상] 다른 사이트는 동일 기간에 예약 가능")
        void 다른_사이트_동일_기간_예약_성공() {
            // given - 사이트 "B-1"이 등록되어 있다 (DatabaseCleanup에서 기본 데이터 복원됨)
            // and - "A-1" 사이트에 "홍길동"의 예약이 있다
            예약_생성_요청("A-1", "2026-01-01", "2026-01-18", "홍길동", 4);

            // when - "김철수"가 "B-1" 사이트를 같은 기간에 예약 요청한다
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "B-1", "2026-01-01", "2026-01-18", "김철수", 4
            );

            // then - 예약이 성공적으로 생성된다
            예약_생성_성공_검증(response, "김철수");
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionCase {

        @Test
        @DisplayName("[예외] 동일 기간 동일 사이트 중복 예약 거부")
        void 중복_예약_시도시_예외_발생() {
            // given - "A-1" 사이트에 "홍길동"의 예약이 있다
            예약_생성_요청("A-1", "2026-01-01", "2026-01-18", "홍길동", 4);

            // when - "김철수"가 같은 기간에 예약 시도한다
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "A-1", "2026-01-01", "2026-01-18", "김철수", 4
            );

            // then - 예약이 거부된다
            // and - 오류 메시지 "해당 기간에 이미 예약이 존재합니다"가 반환된다
            예약_충돌_오류_검증(response);
        }

        @Test
        @DisplayName("[예외] 일부 기간이 겹치는 예약 거부")
        void 기간_겹침_예약_시도시_예외_발생() {
            // given - "A-1" 사이트에 예약이 있다 (1/10 ~ 1/15)
            예약_생성_요청("A-1", "2026-01-10", "2026-01-15", "홍길동", 4);

            // when - 겹치는 기간에 예약 시도한다 (1/14 ~ 1/18)
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "A-1", "2026-01-14", "2026-01-18", "김철수", 4
            );

            // then - 예약이 거부된다
            예약_충돌_오류_검증(response);
        }

        @Test
        @DisplayName("[예외] 동시 예약 요청 시 하나만 성공")
        void 동시_예약_요청시_하나만_성공() throws InterruptedException {
            // given - "A-1" 사이트의 해당 기간에 예약이 없다
            int threadCount = 2;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // when - "김철수"와 "이영희"가 동시에 같은 기간을 예약 요청한다
            Runnable task = () -> {
                try {
                    ExtractableResponse<Response> response = 예약_생성_요청(
                            "A-1", "2026-02-01", "2026-02-05", "고객", 4
                    );
                    if (response.statusCode() == HttpStatus.CREATED.value()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            };

            new Thread(task).start();
            new Thread(task).start();
            latch.await();

            // then - 하나의 예약만 성공한다
            assertThat(successCount.get())
                    .as("동시 요청 시 하나만 성공해야 합니다")
                    .isEqualTo(1);
            assertThat(failCount.get())
                    .as("동시 요청 시 하나는 실패해야 합니다")
                    .isEqualTo(1);
        }
    }
}