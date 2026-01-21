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

@DisplayName("예약 수정 테스트")
class ReservationModifyTest extends AcceptanceTest {

    @Test
    @DisplayName("확정된 예약 날짜 변경 성공")
    void 예약_날짜_수정_성공() {
        // given
        ReservationResult 기존_예약 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(50), 오늘부터_N일_후(52))
                .고객명("홍길동")
                .생성();

        String 새_시작일 = 오늘부터_N일_후(55);
        String 새_종료일 = 오늘부터_N일_후(57);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존_예약.ID(), 기존_예약.확인코드(), 새_시작일, 새_종료일
        );

        // then
        예약_수정_성공_검증(response, 새_시작일, 새_종료일);
    }
}