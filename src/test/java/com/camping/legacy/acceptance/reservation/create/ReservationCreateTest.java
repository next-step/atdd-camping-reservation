package com.camping.legacy.acceptance.reservation.create;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 생성 테스트")
class ReservationCreateTest extends AcceptanceTest {

    @Test
    @DisplayName("예약이 없는 기간에 정상적으로 예약 생성")
    void 예약_생성_성공() {
        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(30), 오늘부터_N일_후(32), "김철수", 4
        );

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("김철수");
    }

    @Test
    @DisplayName("다른 사이트는 동일 기간에 예약 가능")
    void 다른_사이트_동일_기간_예약_성공() {
        // given
        예약_생성_요청("A-1", 오늘부터_N일_후(10), 오늘부터_N일_후(12), "홍길동", 4);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "B-1", 오늘부터_N일_후(10), 오늘부터_N일_후(12), "김철수", 4
        );

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("김철수");
    }
}