package com.camping.legacy.acceptance.reservation.modify;

import com.camping.legacy.common.AcceptanceTest;
import com.camping.legacy.common.ReservationBuilder.ReservationResult;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.*;

@DisplayName("예약 상태 전이 테스트")
class ReservationStateTransitionTest extends AcceptanceTest {

    @Test
    @DisplayName("취소된 예약 수정 거부")
    void 취소된_예약_수정_거부() {
        // given
        ReservationResult 취소할_예약 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(70), 오늘부터_N일_후(72))
                .고객명("홍길동")
                .생성();

        예약_취소_요청(취소할_예약.ID(), 취소할_예약.확인코드());

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                취소할_예약.ID(), 취소할_예약.확인코드(),
                오늘부터_N일_후(75), 오늘부터_N일_후(77)
        );

        // then
        요청_거부_검증(response);
    }

    @Test
    @DisplayName("잘못된 확인코드로 수정 거부")
    void 잘못된_확인코드_수정_거부() {
        // given
        ReservationResult 예약_결과 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(80), 오늘부터_N일_후(82))
                .고객명("홍길동")
                .생성();

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                예약_결과.ID(), "WRONG1",
                오늘부터_N일_후(85), 오늘부터_N일_후(87)
        );

        // then
        요청_거부_검증(response);
    }
}