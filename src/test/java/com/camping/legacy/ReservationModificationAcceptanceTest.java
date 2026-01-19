package com.camping.legacy;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.TestFixture.*;

/**
 * Feature: 취소된 예약 수정 방지
 *
 * @see docs/4.scenarios.feature - "취소된 예약 수정 방지" Feature 참조
 */
@DisplayName("예약 수정 인수 테스트")
class ReservationModificationAcceptanceTest extends AcceptanceTest {

    private Long reservationId;
    private String confirmationCode;

    @BeforeEach
    void setUpFixture() {
        // Background: 사이트와 예약이 존재한다 (DatabaseCleanup에서 기본 데이터 복원됨)
        ExtractableResponse<Response> response = 예약_생성_요청(
                "A-1", 오늘부터_N일_후(50), 오늘부터_N일_후(52), "홍길동", 4
        );
        reservationId = response.jsonPath().getLong("id");
        confirmationCode = response.jsonPath().getString("confirmationCode");
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyPath {

        @Test
        @DisplayName("[정상] 확정된(CONFIRMED) 예약은 수정 가능")
        void 확정_예약_수정_성공() {
            // given - 확인 코드가 있는 예약이 존재한다 (CONFIRMED 상태)
            String newStartDate = 오늘부터_N일_후(55);
            String newEndDate = 오늘부터_N일_후(57);

            // when - 날짜 변경을 요청한다
            ExtractableResponse<Response> response = 예약_수정_요청(
                    reservationId, confirmationCode,
                    newStartDate, newEndDate
            );

            // then - 수정이 성공한다
            // and - 변경된 기간에 맞는 새로운 가격이 계산된다
            예약_수정_성공_검증(response, newStartDate, newEndDate);
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionCase {

        @Test
        @DisplayName("[예외] 취소된(CANCELLED) 예약 수정 시도 거부")
        void 취소된_예약_수정_시도시_실패() {
            // given - 예약이 취소된 상태이다
            예약_취소_요청(reservationId, confirmationCode);

            // when - 취소된 예약에 날짜 변경을 요청한다
            ExtractableResponse<Response> response = 예약_수정_요청(
                    reservationId, confirmationCode,
                    오늘부터_N일_후(60), 오늘부터_N일_후(62)
            );

            // then - 수정이 거부된다
            // and - 오류 메시지 "취소된 예약은 수정할 수 없습니다"가 반환된다
            취소된_예약_수정_거부_검증(response);
        }

        @Test
        @DisplayName("[예외] 당일 취소된(CANCELLED_SAME_DAY) 예약 수정 시도 거부")
        void 당일취소_예약_수정_시도시_실패() {
            // given - 오늘 시작하는 예약을 생성하고 취소한다 (당일 취소 = CANCELLED_SAME_DAY)
            ExtractableResponse<Response> 오늘_예약 = 예약_생성_요청(
                    "A-2", 오늘(), 오늘부터_N일_후(2), "김철수", 2
            );
            Long 오늘_예약_id = 오늘_예약.jsonPath().getLong("id");
            String 오늘_예약_확인코드 = 오늘_예약.jsonPath().getString("confirmationCode");
            예약_취소_요청(오늘_예약_id, 오늘_예약_확인코드);

            // when - 수정을 요청한다
            ExtractableResponse<Response> response = 예약_수정_요청(
                    오늘_예약_id, 오늘_예약_확인코드,
                    오늘부터_N일_후(65), 오늘부터_N일_후(67)
            );

            // then - 수정이 거부된다
            예약_수정_거부_검증(response);
        }

        @Test
        @DisplayName("[예외] 수정 시 다른 예약과 충돌하면 거부")
        void 수정시_다른_예약과_충돌_실패() {
            // given - 다른 예약이 존재한다
            String conflictStart = 오늘부터_N일_후(70);
            String conflictEnd = 오늘부터_N일_후(72);
            예약_생성_요청("A-1", conflictStart, conflictEnd, "김철수", 3);

            // when - 충돌하는 날짜로 변경을 요청한다
            ExtractableResponse<Response> response = 예약_수정_요청(
                    reservationId, confirmationCode,
                    conflictStart, conflictEnd
            );

            // then - 수정이 거부된다
            예약_수정_거부_검증(response);
        }
    }
}