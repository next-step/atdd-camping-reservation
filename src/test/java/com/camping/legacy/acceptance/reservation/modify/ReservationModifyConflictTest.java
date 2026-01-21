package com.camping.legacy.acceptance.reservation.modify;

import com.camping.legacy.common.AcceptanceTest;
import com.camping.legacy.common.ReservationBuilder.ReservationResult;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.오늘부터_N일_후;
import static com.camping.legacy.common.TestFixture.예약_수정_요청;

@DisplayName("예약 수정 충돌 테스트")
class ReservationModifyConflictTest extends AcceptanceTest {

    @Test
    @DisplayName("다른 예약과 충돌하는 날짜로 수정 거부")
    void 충돌_날짜_수정_거부() {
        // given - 첫 번째 예약
        ReservationResult 첫_예약 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(100), 오늘부터_N일_후(102))
                .고객명("홍길동")
                .생성();

        // and - 두 번째 예약 (충돌 대상)
        예약().사이트("A-1")
                .기간(오늘부터_N일_후(105), 오늘부터_N일_후(107))
                .고객명("김철수")
                .생성_요청();

        // when - 첫 번째 예약을 두 번째 예약 기간으로 수정 시도
        ExtractableResponse<Response> response = 예약_수정_요청(
                첫_예약.ID(), 첫_예약.확인코드(),
                오늘부터_N일_후(105), 오늘부터_N일_후(107)
        );

        // then
        요청_거부_검증(response);
    }
}