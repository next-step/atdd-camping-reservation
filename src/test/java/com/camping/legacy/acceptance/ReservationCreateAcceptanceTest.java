package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static com.camping.legacy.acceptance.ReservationRequestBuilder.aReservation;
import static com.camping.legacy.acceptance.ReservationSteps.*;
import static com.camping.legacy.acceptance.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationCreateAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("유효한 정보로 예약 생성 시 201 응답과 확인코드 발급")
    void 예약_생성_성공() {
        // When - 예약 생성 요청
        var response = createReservation(aReservation()
                .withPeriod(1, 3));

        // Then - 응답 검증
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
    }

    @Test
    @DisplayName("시작일과 종료일이 같은 경우 (당일 예약)")
    void 당일_예약_성공() {
        // When - 같은 날 예약 생성 요청
        var response = createReservation(aReservation()
                .withPeriod(5, 5));

        // Then - 응답 검증
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("다른 사이트는 같은 기간 예약 가능")
    void 다른_사이트_같은_기간_예약_성공() {
        // Given - A-1 사이트 예약
        createReservation(aReservation()
                .withSite(SITE_A1)
                .withPeriod(10, 15));

        // When - B-1 사이트 같은 기간 예약
        var response = createReservation(aReservation()
                .withName(OTHER_CUSTOMER)
                .withSite(SITE_B1)
                .withPeriod(10, 15));

        // Then - 성공
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("존재하지 않는 사이트로 예약 시 실패")
    void 존재하지_않는_사이트_예약_실패() {
        // When - 존재하지 않는 사이트로 예약 요청
        var response = createReservation(aReservation()
                .withSite("Z99")
                .withPeriod(1, 3));

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("과거 날짜로 예약 시 실패")
    void 과거_날짜_예약_실패() {
        // When - 과거 날짜로 예약 요청
        var response = createReservation(aReservation()
                .withPeriod(-1, 1));

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("동일 사이트 겹치는 기간 예약 시 실패")
    void 중복_예약_실패() {
        // Given - 첫 번째 예약 생성
        createReservation(aReservation()
                .withSite(SITE_A1)
                .withPeriod(10, 15));

        // When - 겹치는 기간으로 두 번째 예약 시도
        var response = createReservation(aReservation()
                .withName(OTHER_CUSTOMER)
                .withSite(SITE_A1)
                .withPeriod(12, 17));

        // Then - 중복 예약 실패 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전이면 실패")
    void 종료일이_시작일_이전_실패() {
        // When - 종료일 < 시작일로 예약 요청
        var response = createReservation(aReservation()
                .withPeriod(5, 3));

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("30일 초과 기간 예약 시 실패")
    void 최대_예약기간_초과_실패() {
        // When - 31일 이상 기간으로 예약 요청
        var response = createReservation(aReservation()
                .withPeriod(1, 35));

        // Then - 실패 응답 검증
        assertThat(response.statusCode()).isEqualTo(409);
    }

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
                var response = createReservationExpectingFailure("고객" + idx, SITE_A1, 10, 15);
                results.add(response.statusCode());
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        // Then - 하나는 201, 하나는 409
        assertThat(results).containsExactlyInAnyOrder(201, 409);
    }

    @ParameterizedTest(name = "이름 길이 {0}자 → {2}")
    @CsvSource({
            "홍, 1, 409",
            "홍길, 2, 201",
            "홍길동홍길동홍길동홍길동홍길동홍길동홍길, 20, 201",
            "홍길동홍길동홍길동홍길동홍길동홍길동홍길동, 21, 409"
    })
    @DisplayName("이름 길이 경계값 테스트")
    void 이름_길이_경계값(String name, int length, int expectedCode) {
        var response = createReservationWithName(name, SITE_A1, 1, 3);
        assertThat(response.statusCode()).isEqualTo(expectedCode);
    }

    @ParameterizedTest(name = "{0}박 예약 → {1}")
    @CsvSource({
            "1, 201",
            "30, 201",
            "31, 409",
            "35, 409",
    })
    @DisplayName("예약 기간 경계값 테스트")
    void 예약_기간_경계값(int nights, int expectedCode) {
        var response = createReservationWithPeriod(DEFAULT_CUSTOMER, SITE_A1, 1, 1 + nights);
        assertThat(response.statusCode()).isEqualTo(expectedCode);
    }

    @ParameterizedTest(name = "이름: {0} → {1}")
    @CsvSource({
            "'', 409",
            "'   ', 409",
            "' 홍길동 ', 201"
    })
    @DisplayName("예약 기간 경계값 테스트")
    void 이름_입력값_유효성(String name, int expectedCode) {
        var response = createReservationWithName(name, SITE_A1, 1, 3);
        assertThat(response.statusCode()).isEqualTo(expectedCode);
    }

    @ParameterizedTest(name = "시작일 오늘+{0}일 → {1}")
    @CsvSource({
            "-1, 409", // 과거 (어제) - 실패
            "0, 201",  // 오늘 (당일 예약) - 성공
            "1, 201",  // 내일 - 성공
            "30, 201"  // 30일 후 - 성공 (경계)
    })
    @DisplayName("예약 시작일 경계값 테스트")
    void 예약_시작일_경계값(int startDaysFromNow, int expectedCode) {
        var response = createReservationWithPeriod(DEFAULT_CUSTOMER, SITE_A1, startDaysFromNow, startDaysFromNow + 1);
        assertThat(response.statusCode()).isEqualTo(expectedCode);
    }

}
