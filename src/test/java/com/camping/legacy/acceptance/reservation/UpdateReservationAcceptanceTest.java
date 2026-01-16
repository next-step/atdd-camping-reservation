package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.ApiAcceptanceTestBase;
import com.camping.legacy.acceptance.fixtures.ReservationRequestFixture;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.fixtures.TestFixtures.기본_예약_요청;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see "file:docs/features/update-reservation.feature"
 */
@SuppressWarnings("NonAsciiCharacters")
class UpdateReservationAcceptanceTest extends ApiAcceptanceTestBase {

    /**
     * FIXME: 기간은 필수값인데 현재 버그니 수정바람
     */
    @Test
    void 올바른_확인코드면_예약자_이름을_수정할_수_있다() {
        ExtractableResponse<Response> response = 예약을_생성한다(기본_예약_요청);
        Long reservationId = response.jsonPath().getLong("id");
        String confirmationCode = response.jsonPath().getString("confirmationCode");

        ExtractableResponse<Response> updated = 예약을_수정한다(
                reservationId,
                confirmationCode,
                ReservationRequestFixture.builder()
                        .customerName("수정한 이름")
                        .startDate(null)
                        .endDate(null)
                        .siteNumber("A-1")
                        .build()
        );

        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.jsonPath().getString("customerName")).isEqualTo("수정한 이름");
    }

    @Test
    void 확인코드가_일치하지_않으면_예약_수정이_거부된다() {
        ExtractableResponse<Response> created = 예약을_생성한다(기본_예약_요청);
        Long reservationId = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        ExtractableResponse<Response> response = 예약을_수정한다(
                reservationId,
                confirmationCode + "WRONG",
                ReservationRequestFixture.builder()
                        .customerName("수정한 이름")
                        .startDate(null)
                        .endDate(null)
                        .siteNumber("A-1")
                        .build()
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("확인 코드가 일치하지 않습니다.");
    }

    @Test
    void 변경된_날짜가_과거라면_예약_수정이_거부된다() {
        ExtractableResponse<Response> created = 예약을_생성한다(기본_예약_요청);
        Long reservationId = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        // When: 과거로 변경 요청
        LocalDate pastStart = LocalDate.now().minusDays(3);
        LocalDate pastEnd = LocalDate.now().minusDays(1);

        ExtractableResponse<Response> response = 예약을_수정한다(
                reservationId,
                confirmationCode,
                ReservationRequestFixture.builder()
                        .customerName("수정한 이름")
                        .startDate(pastStart)
                        .endDate(pastEnd)
                        .siteNumber("A-1")
                        .build()
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("과거 날짜로 예약할 수 없습니다.");
    }

    // FIXME 현재 updateReservation에는 '중복 예약 방지' 로직이 없음
    @Disabled
    @Test
    void 변경된_예약이_다른_예약과_기간이_겹치면_수정이_거부된다() {
        // 이미 존재하는 예약 (겹치게 만들 대상)
        LocalDate conflictStart = LocalDate.now().plusDays(20);
        LocalDate conflictEnd = LocalDate.now().plusDays(22);
        ExtractableResponse<Response> conflictCreated = 예약을_생성한다(
                ReservationRequestFixture.builder()
                        .customerName("홍길동")
                        .startDate(conflictStart)
                        .endDate(conflictEnd)
                        .siteNumber("A-1")
                        .build()
        );
        assertThat(conflictCreated.statusCode()).isEqualTo(201);

        // 수정 대상 예약
        LocalDate targetStart = LocalDate.now().plusDays(24);
        LocalDate targetEnd = LocalDate.now().plusDays(25);
        ExtractableResponse<Response> targetCreated = 예약을_생성한다(
                ReservationRequestFixture.builder()
                        .customerName("김철수")
                        .startDate(targetStart)
                        .endDate(targetEnd)
                        .siteNumber("A-1")
                        .build()
        );

        Long targetId = targetCreated.jsonPath().getLong("id");
        String targetCode = targetCreated.jsonPath().getString("confirmationCode");

        // (기간 겹침)
        ExtractableResponse<Response> response = 예약을_수정한다(
                targetId,
                targetCode,
                ReservationRequestFixture.builder()
                        .customerName("수정한 이름")
                        .startDate(conflictStart)
                        .endDate(conflictEnd)
                        .siteNumber("A-1")
                        .build()
        );

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    // FIXME 현재 updateReservation에는 '30일 제한' 로직이 없음
    @Disabled
    @Test
    void 예외_변경된_예약_기간이_30일을_초과하면_수정이_거부된다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        ExtractableResponse<Response> created = 예약을_생성한다(
                ReservationRequestFixture.builder()
                        .customerName("김철수")
                        .startDate(start)
                        .endDate(end)
                        .siteNumber("A-1")
                        .build());

        Long id = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        // When: 30일 초과로 변경
        LocalDate newStart = LocalDate.now().plusDays(10);
        LocalDate newEnd = newStart.plusDays(31);

        ExtractableResponse<Response> response = 예약을_수정한다(
                id,
                confirmationCode,
                ReservationRequestFixture.builder()
                        .customerName("수정한 이름")
                        .startDate(newStart)
                        .endDate(newEnd)
                        .siteNumber("A-1")
                        .build()
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약 기간은 최대 30일입니다.");
    }

}

