package com.camping.legacy.acceptance.reservation.modify;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 수정 충돌 테스트")
class ReservationModifyConflictTest extends AcceptanceTest {

    @Test
    @DisplayName("다른 예약과 충돌하는 날짜로 수정 거부")
    void 충돌_날짜_수정_거부() {
        // given
        ExtractableResponse<Response> 예약1 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(100), 오늘부터_N일_후(102), "홍길동", 4
        );
        Long 예약1_id = 예약1.jsonPath().getLong("id");
        String 확인코드 = 예약1.jsonPath().getString("confirmationCode");

        // and
        예약_생성_요청("A-1", 오늘부터_N일_후(105), 오늘부터_N일_후(107), "김철수", 4);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                예약1_id, 확인코드, 오늘부터_N일_후(105), 오늘부터_N일_후(107)
        );

        // then
        assertThat(response.statusCode()).isBetween(400, 499);
    }
}