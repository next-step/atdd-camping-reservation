package com.camping.legacy.acceptance.reservation.cancel;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 권한 검증 테스트")
class ReservationAuthorizationTest extends AcceptanceTest {

    @Test
    @DisplayName("잘못된 확인코드로 취소 거부")
    void 잘못된_확인코드_취소_거부() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(90), 오늘부터_N일_후(92), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_id, "WRONG1");

        // then
        assertThat(response.statusCode()).isBetween(400, 499);
    }
}