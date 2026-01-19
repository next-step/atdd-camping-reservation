package com.camping.legacy.acceptance.reservation.cancel;

import com.camping.legacy.common.AcceptanceTest;
import com.camping.legacy.common.ReservationBuilder.ReservationResult;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.오늘부터_N일_후;
import static com.camping.legacy.common.TestFixture.예약_취소_요청;

@DisplayName("예약 권한 검증 테스트")
class ReservationAuthorizationTest extends AcceptanceTest {

    @Test
    @DisplayName("잘못된 확인코드로 취소 거부")
    void 잘못된_확인코드_취소_거부() {
        // given
        ReservationResult 예약_결과 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(90), 오늘부터_N일_후(92))
                .고객명("홍길동")
                .생성();

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_결과.ID(), "WRONG1");

        // then
        요청_거부_검증(response);
    }
}