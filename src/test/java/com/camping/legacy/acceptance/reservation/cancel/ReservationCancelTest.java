package com.camping.legacy.acceptance.reservation.cancel;

import com.camping.legacy.common.AcceptanceTest;
import com.camping.legacy.common.ReservationBuilder.ReservationResult;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.*;

@DisplayName("예약 취소 테스트")
class ReservationCancelTest extends AcceptanceTest {

    @Test
    @DisplayName("확정된 예약 취소 성공")
    void 예약_취소_성공() {
        // given
        ReservationResult 예약_결과 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(60), 오늘부터_N일_후(62))
                .고객명("홍길동")
                .생성();

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_결과.ID(), 예약_결과.확인코드());

        // then
        예약_취소_성공_검증(response);
    }

    @Test
    @DisplayName("취소 후 해당 기간 재예약 가능")
    void 취소_후_재예약_성공() {
        // given
        ReservationResult 취소할_예약 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(65), 오늘부터_N일_후(67))
                .고객명("홍길동")
                .생성();

        예약_취소_요청(취소할_예약.ID(), 취소할_예약.확인코드());

        // when
        ExtractableResponse<Response> response = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(65), 오늘부터_N일_후(67))
                .고객명("김철수")
                .생성_요청();

        // then
        예약_생성_성공_검증(response, "김철수");
    }

    @Test
    @DisplayName("이미 취소된 예약 재취소 시도 - 멱등성")
    void 이미_취소된_예약_재취소() {
        // given
        ReservationResult 예약_결과 = 예약()
                .사이트("A-1")
                .기간(오늘부터_N일_후(95), 오늘부터_N일_후(97))
                .고객명("홍길동")
                .생성();

        예약_취소_요청(예약_결과.ID(), 예약_결과.확인코드());

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(예약_결과.ID(), 예약_결과.확인코드());

        // then - 멱등성: 이미 취소된 상태이므로 성공 또는 적절한 응답
        예약_취소_성공_검증(response);
    }

    @Test
    @DisplayName("당일 예약 취소 처리")
    void 당일_예약_취소() {
        // given
        ReservationResult 당일_예약 = 예약()
                .사이트("A-1")
                .기간(오늘(), 오늘부터_N일_후(2))
                .고객명("홍길동")
                .생성();

        // when
        ExtractableResponse<Response> response = 예약_취소_요청(당일_예약.ID(), 당일_예약.확인코드());

        // then - 당일 취소도 성공해야 함
        예약_취소_성공_검증(response);
    }
}