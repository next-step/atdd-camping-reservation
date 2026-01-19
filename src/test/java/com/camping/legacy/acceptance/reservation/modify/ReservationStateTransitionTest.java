package com.camping.legacy.acceptance.reservation.modify;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 상태 전이 테스트")
class ReservationStateTransitionTest extends AcceptanceTest {

    @Test
    @DisplayName("취소된 예약 수정 거부")
    void 취소된_예약_수정_거부() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(70), 오늘부터_N일_후(72), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");
        String 확인코드 = 예약.jsonPath().getString("confirmationCode");
        예약_취소_요청(예약_id, 확인코드);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                예약_id, 확인코드, 오늘부터_N일_후(75), 오늘부터_N일_후(77)
        );

        // then
        assertThat(response.statusCode()).isBetween(400, 499);
    }

    @Test
    @DisplayName("잘못된 확인코드로 수정 거부")
    void 잘못된_확인코드_수정_거부() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(80), 오늘부터_N일_후(82), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                예약_id, "WRONG1", 오늘부터_N일_후(85), 오늘부터_N일_후(87)
        );

        // then
        assertThat(response.statusCode()).isBetween(400, 499);
    }
}