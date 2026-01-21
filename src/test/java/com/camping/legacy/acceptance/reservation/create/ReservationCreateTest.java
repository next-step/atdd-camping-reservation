package com.camping.legacy.acceptance.reservation.create;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.오늘부터_N일_후;

@DisplayName("예약 생성 테스트")
class ReservationCreateTest extends AcceptanceTest {

    @Test
    @DisplayName("예약이 없는 기간에 정상적으로 예약 생성")
    void 예약_생성_성공() {
        // when
        ExtractableResponse<Response> response = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(30), 오늘부터_N일_후(32))
                .고객명("김철수")
                .생성_요청();

        // then
        예약_생성_성공_검증(response, "김철수");
    }

    @Test
    @DisplayName("다른 사이트는 동일 기간에 예약 가능")
    void 다른_사이트_동일_기간_예약_성공() {
        // given
        예약().사이트("A-1")
                .기간(오늘부터_N일_후(10), 오늘부터_N일_후(12))
                .고객명("홍길동")
                .생성_요청();

        // when
        ExtractableResponse<Response> response = 예약()
                .사이트("B-1")
                .기간(오늘부터_N일_후(10), 오늘부터_N일_후(12))
                .고객명("김철수")
                .생성_요청();

        // then
        예약_생성_성공_검증(response, "김철수");
    }
}