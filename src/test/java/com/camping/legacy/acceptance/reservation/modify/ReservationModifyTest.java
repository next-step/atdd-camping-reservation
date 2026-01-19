package com.camping.legacy.acceptance.reservation.modify;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 수정 테스트")
class ReservationModifyTest extends AcceptanceTest {

    @Test
    @DisplayName("확정된 예약 날짜 변경 성공")
    void 예약_날짜_수정_성공() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(50), 오늘부터_N일_후(52), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");
        String 확인코드 = 예약.jsonPath().getString("confirmationCode");

        String newStartDate = 오늘부터_N일_후(55);
        String newEndDate = 오늘부터_N일_후(57);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                예약_id, 확인코드, newStartDate, newEndDate
        );

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(newStartDate);
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(newEndDate);
    }
}