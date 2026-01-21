package com.camping.legacy.acceptance.reservation.create;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.오늘부터_N일_후;

@DisplayName("예약 충돌 테스트")
class ReservationConflictTest extends AcceptanceTest {

    @Test
    @DisplayName("동일 기간 동일 사이트 중복 예약 거부")
    void 중복_예약_거부() {
        // given
        예약().사이트("A-1")
                .기간(오늘부터_N일_후(15), 오늘부터_N일_후(17))
                .고객명("홍길동")
                .생성_요청();

        // when
        ExtractableResponse<Response> response = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(15), 오늘부터_N일_후(17))
                .고객명("김철수")
                .생성_요청();

        // then
        충돌_검증(response);
    }

    @Test
    @DisplayName("일부 기간이 겹치는 예약 거부")
    void 기간_겹침_예약_거부() {
        // given
        예약().사이트("A-1")
                .기간(오늘부터_N일_후(20), 오늘부터_N일_후(25))
                .고객명("홍길동")
                .생성_요청();

        // when
        ExtractableResponse<Response> response = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(24), 오늘부터_N일_후(28))
                .고객명("김철수")
                .생성_요청();

        // then
        충돌_검증(response);
    }
}