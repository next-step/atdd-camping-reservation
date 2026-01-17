package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.ApiAcceptanceTestBase;
import com.camping.legacy.acceptance.fixtures.ReservationRequest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static com.camping.legacy.acceptance.fixtures.ReservationRequestFactory.같은_기간_다른_고객;
import static com.camping.legacy.acceptance.fixtures.TestFixtures.*;
import static com.camping.legacy.acceptance.matcher.AcceptanceAssertions.assertThatResponse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see "file:docs/features/create-reservation.feature"
 */
@SuppressWarnings("NonAsciiCharacters")
class ReservationAcceptanceTest extends ApiAcceptanceTestBase {

    @Test
    void 예약_생성() {
        ExtractableResponse<Response> response = 예약을_생성한다(기본_예약_요청);

        assertThatResponse(response)
                .status(201)
                .response(it -> {
                    assertThat(it.getString("status")).isEqualTo("CONFIRMED");

                    String confirmationCode = it.getString("confirmationCode");
                    assertThat(confirmationCode).isNotBlank();
                    assertThat(confirmationCode).matches("^[A-Z0-9]{6}$");
                });
    }

    @Test
    void 예외_종료일이_시작일보다_이전이면_예약이_거부된다() {
        ExtractableResponse<Response> response = 예약을_생성한다(종료일이_시작일_보다_빠른_예약);

        assertThatResponse(response)
                .status(409)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("종료일이 시작일보다 이전일 수 없습니다."));
    }

    @Test
    void 예외_과거_날짜로_예약을_시도하면_거부된다() {
        ExtractableResponse<Response> response = 예약을_생성한다(과거_시간의_예약);

        assertThatResponse(response)
                .status(409)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("과거 날짜로 예약할 수 없습니다."));
    }

    @Test
    void 예외_허용된_기간_30일을_초과하면_예약이_거부된다() {
        ExtractableResponse<Response> response = 예약을_생성한다(기간이_30일_초과된_예약);

        assertThatResponse(response)
                .status(409)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("예약 기간은 최대 30일입니다."));
    }

    @Test
    void 예외_동일_사이트의_기간이_겹치면_중복_예약이_거부된다() {
        예약을_생성한다(기본_예약_요청);

        ExtractableResponse<Response> response = 예약을_생성한다(같은_기간_다른_고객(기본_예약_요청, "김철수"));

        // Then
        assertThatResponse(response)
                .status(409)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("해당 기간에 이미 예약이 존재합니다."));
    }

    // FIXME: 동시성 이슈 해결 필요
    @Test
    @Disabled
    void 예외_동시에_동일_사이트_기간으로_예약_요청이_여러_건_들어와도_하나만_성공해야_한다() throws ExecutionException, InterruptedException {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task1 = () -> 예약을_생성한다(ReservationRequest.builder()
                    .customerName("사용자1")
                    .startDate(startDate)
                    .endDate(endDate)
                    .siteNumber("A-1")
                    .build()).statusCode();
            Callable<Integer> task2 = () -> 예약을_생성한다(ReservationRequest.builder()
                    .customerName("사용자2")
                    .startDate(startDate)
                    .endDate(endDate)
                    .siteNumber("A-1")
                    .build()).statusCode();

            List<Future<Integer>> futures = pool.invokeAll(List.of(task1, task2));

            int s1 = futures.get(0).get();
            int s2 = futures.get(1).get();

            // 성공: 201, 중복 거부: 409 (컨트롤러 정책)
            assertThat(List.of(s1, s2)).contains(201);
            assertThat(List.of(s1, s2)).contains(409);
        } finally {
            pool.shutdownNow();
        }
    }
}
