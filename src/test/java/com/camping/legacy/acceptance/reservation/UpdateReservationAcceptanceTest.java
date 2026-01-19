package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.ApiAcceptanceTestBase;
import com.camping.legacy.acceptance.fixtures.ReservationRequest;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static com.camping.legacy.acceptance.fixtures.TestFixtures.기본_예약_요청;
import static com.camping.legacy.acceptance.matcher.AcceptanceAssertions.assertThatResponse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see "file:docs/features/update-reservation.feature"
 */
@SuppressWarnings("NonAsciiCharacters")
class UpdateReservationAcceptanceTest extends ApiAcceptanceTestBase {

    @BeforeEach
    void 배경_데이터_설정() {
        사이트를_생성한다("A-1");
    }

    @Test
    void 올바른_확인코드면_예약자_이름을_수정할_수_있다() {
        JsonPath reservation = 예약을_생성한다(기본_예약_요청).jsonPath();
        Long reservationId = reservation.getLong("id");
        String confirmationCode = reservation.getString("confirmationCode");

        ExtractableResponse<Response> updated = 예약을_수정한다(
                reservationId,
                confirmationCode,
                ReservationRequest.builder()
                        .customerName("수정한 이름")
                        .startDate(LocalDate.now().plusDays(2))
                        .endDate(LocalDate.now().plusDays(4))
                        .siteNumber("A-1")
                        .build()
        );

        assertThatResponse(updated)
                .status(200)
                .response(it -> assertThat(it.getString("customerName")).isEqualTo("수정한 이름"));
    }

    @Test
    void 확인코드가_일치하지_않으면_예약_수정이_거부된다() {
        JsonPath reservation = 예약을_생성한다(기본_예약_요청).jsonPath();
        Long reservationId = reservation.getLong("id");
        String confirmationCode = reservation.getString("confirmationCode");

        ExtractableResponse<Response> response = 예약을_수정한다(
                reservationId,
                confirmationCode + "WRONG",
                ReservationRequest.builder()
                        .customerName("수정한 이름")
                        .startDate(null)
                        .endDate(null)
                        .siteNumber("A-1")
                        .build()
        );

        assertThatResponse(response)
                .status(400)
                .response(it -> assertThat(it.getString("message")).isEqualTo("확인 코드가 일치하지 않습니다."));
    }

    @Test
    void 변경된_날짜가_과거라면_예약_수정이_거부된다() {
        JsonPath reservation = 예약을_생성한다(기본_예약_요청).jsonPath();
        Long reservationId = reservation.getLong("id");
        String confirmationCode = reservation.getString("confirmationCode");

        LocalDate pastStart = LocalDate.now().minusDays(3);
        LocalDate pastEnd = LocalDate.now().minusDays(1);

        ExtractableResponse<Response> response = 예약을_수정한다(
                reservationId,
                confirmationCode,
                ReservationRequest.builder()
                        .customerName("수정한 이름")
                        .startDate(pastStart)
                        .endDate(pastEnd)
                        .siteNumber("A-1")
                        .build()
        );

        assertThatResponse(response)
                .status(400)
                .response(it -> assertThat(it.getString("message")).isEqualTo("과거 날짜로 예약할 수 없습니다."));
    }

    @Test
    void 변경된_예약이_다른_예약과_기간이_겹치면_수정이_거부된다() {
        LocalDate conflictStart = LocalDate.now().plusDays(20);
        LocalDate conflictEnd = LocalDate.now().plusDays(22);

        // 먼저 충돌하는 예약을 생성한다
        ExtractableResponse<Response> conflictCreated = 예약을_생성한다(
                ReservationRequest.builder()
                        .customerName("홍길동")
                        .startDate(conflictStart)
                        .endDate(conflictEnd)
                        .siteNumber("A-1")
                        .build()
        );
        assertThatResponse(conflictCreated).status(201);

        // 그 다음 수정할 예약을 생성한다
        JsonPath targetCreated = 예약을_생성한다(
                ReservationRequest.builder()
                        .customerName("김철수")
                        .startDate(LocalDate.now().plusDays(24))
                        .endDate(LocalDate.now().plusDays(25))
                        .siteNumber("A-1")
                        .build()
        ).jsonPath();

        Long targetId = targetCreated.getLong("id");
        String targetCode = targetCreated.getString("confirmationCode");

        ExtractableResponse<Response> response = 예약을_수정한다(
                targetId,
                targetCode,
                ReservationRequest.builder()
                        .customerName("수정한 이름")
                        .startDate(conflictStart)
                        .endDate(conflictEnd)
                        .siteNumber("A-1")
                        .build()
        );

        assertThatResponse(response)
                .status(400)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("해당 기간에 이미 예약이 존재합니다."));
    }

    @Test
    void 예외_변경된_예약_기간이_30일을_초과하면_수정이_거부된다() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);

        JsonPath created = 예약을_생성한다(
                ReservationRequest.builder()
                        .customerName("김철수")
                        .startDate(start)
                        .endDate(end)
                        .siteNumber("A-1")
                        .build()
        ).jsonPath();

        Long id = created.getLong("id");
        String confirmationCode = created.getString("confirmationCode");

        LocalDate newStart = LocalDate.now().plusDays(10);
        LocalDate newEnd = newStart.plusDays(31);

        ExtractableResponse<Response> response = 예약을_수정한다(
                id,
                confirmationCode,
                ReservationRequest.builder()
                        .customerName("수정한 이름")
                        .startDate(newStart)
                        .endDate(newEnd)
                        .siteNumber("A-1")
                        .build()
        );

        assertThatResponse(response)
                .status(400)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("예약 기간은 최대 30일입니다."));
    }

    @Test
    void 예외_동시에_서로_다른_예약을_동일_사이트_기간으로_수정하면_하나만_성공해야_한다() throws Exception {
        JsonPath res1 = 예약을_생성한다(ReservationRequest.builder()
                .customerName("사용자1")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .siteNumber("A-1")
                .build()).jsonPath();

        JsonPath res2 = 예약을_생성한다(ReservationRequest.builder()
                .customerName("사용자2")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(16))
                .siteNumber("A-1")
                .build()).jsonPath();

        Long id1 = res1.getLong("id");
        String code1 = res1.getString("confirmationCode");

        Long id2 = res2.getLong("id");
        String code2 = res2.getString("confirmationCode");

        LocalDate targetStart = LocalDate.now().plusDays(20);
        LocalDate targetEnd = LocalDate.now().plusDays(22);

        CyclicBarrier barrier = new CyclicBarrier(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task1 = () -> {
                barrier.await(); // 두 스레드 모두 여기 도착해야 동시에 진행
                return 예약을_수정한다(
                        id1, code1,
                        ReservationRequest.builder()
                                .customerName("사용자1")
                                .startDate(targetStart)
                                .endDate(targetEnd)
                                .siteNumber("A-1")
                                .build()
                ).statusCode();
            };

            Callable<Integer> task2 = () -> {
                barrier.await();
                return 예약을_수정한다(
                        id2, code2,
                        ReservationRequest.builder()
                                .customerName("사용자2")
                                .startDate(targetStart)
                                .endDate(targetEnd)
                                .siteNumber("A-1")
                                .build()
                ).statusCode();
            };

            Future<Integer> f1 = pool.submit(task1);
            Future<Integer> f2 = pool.submit(task2);

            assertThat(List.of(f1.get(), f2.get())).contains(200, 400);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void 예외_예약_기간_변경_시_종료일_없는경우_수정이_거부된다() {
        JsonPath reservation = 예약을_생성한다(기본_예약_요청).jsonPath();
        Long reservationId = reservation.getLong("id");
        String confirmationCode = reservation.getString("confirmationCode");

        // When: 시작일만 변경 시도
        ExtractableResponse<Response> responseStartOnly = 예약을_수정한다(
                reservationId,
                confirmationCode,
                ReservationRequest.builder()
                        .customerName("김철수")
                        .startDate(LocalDate.now().plusDays(15))
                        .endDate(null)
                        .siteNumber("A-1")
                        .build()
        );

        // Then: 거부됨
        assertThatResponse(responseStartOnly)
                .status(400)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("예약 기간을 선택해주세요."));
    }

    @Test
    void 예외_예약_기간_변경_시_시작일_없는경우_수정이_거부된다() {
        JsonPath reservation = 예약을_생성한다(기본_예약_요청).jsonPath();
        Long reservationId = reservation.getLong("id");
        String confirmationCode = reservation.getString("confirmationCode");

        // When: 종료일만 변경 시도
        ExtractableResponse<Response> responseEndOnly = 예약을_수정한다(
                reservationId,
                confirmationCode,
                ReservationRequest.builder()
                        .customerName("김철수")
                        .startDate(null)
                        .endDate(LocalDate.now().plusDays(15))
                        .siteNumber("A-1")
                        .build()
        );

        // Then: 거부됨
        assertThatResponse(responseEndOnly)
                .status(400)
                .response(it -> assertThat(it.getString("message"))
                        .isEqualTo("예약 기간을 선택해주세요."));
    }
}
