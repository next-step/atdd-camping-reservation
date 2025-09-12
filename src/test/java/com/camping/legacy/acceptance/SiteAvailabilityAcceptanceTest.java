package com.camping.legacy.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import com.camping.legacy.acceptance.support.ReservationApiHelper;
import com.camping.legacy.acceptance.support.ReservationTestDataBuilder;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
class SiteAvailabilityAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 예약된_캠핑_구역은_예약_불가능으로_표시() {
        // given - A-3 캠핑 구역이 12월 25일에 이미 예약되어 있을 때
        LocalDate reservedDate = LocalDate.of(2025, 12, 25);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-3")
            .withDates(reservedDate, reservedDate.plusDays(1))
            .build();

        ReservationApiHelper.createReservation(existingReservation);

        // when - 고객이 12월 25일에 A-3 캠핑 구역의 예약 가능 여부를 확인하면
        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-3", reservedDate);

        // then - "예약 불가능"으로 표시된다
        assertThat(response.statusCode()).isEqualTo(OK.value());
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
    }

    @Test
    void 비어있는_캠핑_구역은_예약_가능으로_표시() {
        // when - 고객이 12월 25일에 A-4 캠핑 구역의 예약 가능 여부를 확인하면
        LocalDate availableDate = LocalDate.of(2025, 12, 25);

        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-4", availableDate);

        // then - "예약 가능"으로 표시된다
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
    }

    @Test
    void 예약_완료_즉시_다른_고객에게는_예약_불가능으로_표시() {
        // when - 고객A가 A-6 캠핑 구역을 12월 25일에 예약한 직후
        LocalDate targetDate = LocalDate.of(2025, 12, 25);

        Map<String, Object> reservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-6")
            .withDates(targetDate, targetDate.plusDays(1))
            .withName("고객A")
            .build();

        ReservationApiHelper.createReservation(reservation);

        // and - 고객B가 같은 날짜의 예약 가능 여부를 확인하면
        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-6", targetDate);

        // then - "예약 불가능"으로 표시된다
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
    }

    @Test
    void 여러_명이_동시에_확인해도_동일한_정보_제공() {
        // given - A-8 캠핑 구역이 12월 25일에 예약되어 있을 때
        LocalDate checkDate = LocalDate.of(2025, 12, 25);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new ReservationTestDataBuilder()
            .withSiteNumber("A-8")
            .withDates(checkDate, checkDate.plusDays(1))
            .build();

        ReservationApiHelper.createReservation(existingReservation);

        // when - 10명의 고객이 동시에 A-8 캠핑 구역의 예약 가능 여부를 확인하면
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                try {
                    ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-8", checkDate);

                    synchronized (responses) {
                        responses.add(response);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executorService.shutdown();

        // then - 모든 고객에게 동일하게 "예약 불가능"으로 표시된다
        assertThat(responses).hasSize(10);

        for (ExtractableResponse<Response> response : responses) {
            assertThat(response.jsonPath().getBoolean("available")).isFalse();
        }
    }

    @Test
    void 존재하지_않는_캠핑장_가용성_조회_실패() {
        // when - 고객이 존재하지 않는 캠핑장의 가용성을 조회하면
        LocalDate queryDate = LocalDate.now().plusDays(10);

        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("Z-99", queryDate);

        // then - "사이트를 찾을 수 없습니다"라는 안내 메시지가 나타난다
        assertThat(response.jsonPath().getString("message").contains("사이트를 찾을 수 없습니다"));
    }

    @Test
    void 단독_가용성_조회_성공() {
        // when - 1명의 고객이 A-9 캠핑 구역의 가용성을 조회하면
        LocalDate availableDate = LocalDate.now().plusDays(20);

        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-9", availableDate);

        // then - "예약 가능"으로 표시된다
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
    }

    @Test
    void 최소_동시_가용성_조회_2명() {
        // when - 2명의 고객이 동시에 A-10 캠핑 구역의 예약 가능 여부를 확인하면
        LocalDate checkDate = LocalDate.now().plusDays(16);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            executorService.execute(() -> {
                try {
                    ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-10", checkDate);

                    synchronized (responses) {
                        responses.add(response);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executorService.shutdown();

        // then - 모든 고객에게 동일하게 "예약 가능"으로 표시된다
        assertThat(responses).hasSize(2);

        for (ExtractableResponse<Response> response : responses) {
            assertThat(response.statusCode()).isEqualTo(OK.value());
            assertThat(response.jsonPath().getBoolean("available")).isTrue();
        }
    }

    @Test
    void 오늘_날짜_가용성_조회_성공() {
        // when - 고객이 오늘 날짜로 A-11 캠핑 구역의 가용성을 조회하면
        LocalDate today = LocalDate.now();

        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability("A-11", today);

        // then - "예약 가능"으로 표시된다
        assertThat(response.statusCode()).isEqualTo(OK.value());
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
    }

    @Test
    void 빈_캠핑장_번호로_가용성_조회_실패() {
        // when - 고객이 빈 캠핑장 번호로 가용성을 조회하면
        LocalDate queryDate = LocalDate.now().plusDays(10);

        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailability(" ", queryDate);

        // then - "사이트를 찾을 수 없습니다"라는 안내 메시지가 나타난다
        assertThat(response.jsonPath().getString("message")).contains("사이트를 찾을 수 없습니다");
    }

    @Test
    void 날짜_파라미터_없이_가용성_조회_실패() {
        // when - 고객이 날짜 없이 가용성을 조회하면
        ExtractableResponse<Response> response = ReservationApiHelper.checkSiteAvailabilityWithoutDateParam("A-12");

        // then - "필수 파라미터가 누락되었습니다"라는 안내 메시지가 나타난다
        assertThat(response.jsonPath().getString("message")).contains("date");
    }
}
