package com.camping.legacy.acceptance.reservation.cancel;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 취소 테스트")
class ReservationCancelTest extends AcceptanceTest {

    @Test
    @DisplayName("확정된 예약 취소 성공")
    void 예약_취소_성공() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(60), 오늘부터_N일_후(62), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");
        String 확인코드 = 예약.jsonPath().getString("confirmationCode");

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_id, 확인코드);

        // then
        assertThat(response.statusCode()).isIn(HttpStatus.OK.value(), HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("취소 후 해당 기간 재예약 가능")
    void 취소_후_재예약_성공() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(65), 오늘부터_N일_후(67), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");
        String 확인코드 = 예약.jsonPath().getString("confirmationCode");
        예약_취소_요청(예약_id, 확인코드);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(65), 오늘부터_N일_후(67), "김철수", 4
        );

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("김철수");
    }

    @Test
    @DisplayName("이미 취소된 예약 재취소 시도 - 멱등성")
    void 이미_취소된_예약_재취소() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(95), 오늘부터_N일_후(97), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");
        String 확인코드 = 예약.jsonPath().getString("confirmationCode");
        예약_취소_요청(예약_id, 확인코드);

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_id, 확인코드);

        // then - 멱등성: 이미 취소된 상태이므로 성공 또는 적절한 응답
        assertThat(response.statusCode()).isIn(HttpStatus.OK.value(), HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("당일 예약 취소 처리")
    void 당일_예약_취소() {
        // given
        ExtractableResponse<Response> 예약 = 예약_생성_요청(
                "A-1", 오늘(), 오늘부터_N일_후(2), "홍길동", 4
        );
        Long 예약_id = 예약.jsonPath().getLong("id");
        String 확인코드 = 예약.jsonPath().getString("confirmationCode");

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_id, 확인코드);

        // then - 당일 취소도 성공해야 함
        assertThat(response.statusCode()).isIn(HttpStatus.OK.value(), HttpStatus.NO_CONTENT.value());
    }
}