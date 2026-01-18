package com.camping.legacy;

import com.camping.legacy.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.common.CalendarAssertions.*;
import static com.camping.legacy.common.ReservationAssertions.*;
import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: 취소된 예약 캘린더 제외
 *
 * @see docs/4.scenarios.feature - "취소된 예약 캘린더 제외" Feature 참조
 */
@DisplayName("캘린더 조회 인수 테스트")
class CalendarAcceptanceTest extends AcceptanceTest {

    private static final int JANUARY_DAYS = 31;

    private Long siteId;

    @BeforeEach
    void setUpFixture() {
        // Background: 사이트 "A-1"이 등록되어 있다 (DatabaseCleanup에서 기본 데이터 복원됨)
        siteId = 사이트_ID_조회("A-1");
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyPath {

        @Test
        @DisplayName("[정상] 취소된 예약은 캘린더에서 이용 가능으로 표시")
        void 취소된_예약_이용가능_표시() {
            // given - "A-1" 사이트에 취소된 예약이 있다
            ExtractableResponse<Response> 예약_응답 = 예약_생성_요청(
                    "A-1", "2026-01-20", "2026-01-22", "홍길동", 4);
            Long 예약_id = 예약_응답.jsonPath().getLong("id");
            String 확인코드 = 예약_응답.jsonPath().getString("confirmationCode");
            예약_취소_요청(예약_id, 확인코드);

            // when - 관리자가 캘린더를 조회한다
            ExtractableResponse<Response> response = 캘린더_조회_요청(siteId, 2026, 1);

            // then - 해당 날짜들은 "이용 가능" 상태이다
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            날짜가_예약가능_검증(response, "2026-01-20", "2026-01-21");
        }

        @Test
        @DisplayName("[정상] 취소된 날짜에 새로운 예약 가능")
        void 취소된_날짜_재예약_성공() {
            // given - "A-1" 사이트에 취소된 예약이 있다
            ExtractableResponse<Response> 예약_응답 = 예약_생성_요청(
                    "A-1", "2026-01-20", "2026-01-22", "홍길동", 4);
            Long 예약_id = 예약_응답.jsonPath().getLong("id");
            String 확인코드 = 예약_응답.jsonPath().getString("confirmationCode");
            예약_취소_요청(예약_id, 확인코드);

            // when - 취소된 날짜에 새로운 예약을 요청한다
            ExtractableResponse<Response> 재예약_응답 = 예약_생성_요청(
                    "A-1", "2026-01-20", "2026-01-22", "김철수", 3);

            // then - 예약이 성공적으로 생성된다
            예약_생성_성공_검증(재예약_응답, "김철수");
        }

        @Test
        @DisplayName("[정상] 확정된 예약만 캘린더에 예약됨으로 표시")
        void 확정_예약만_캘린더_표시() {
            // given - 확정된 예약과 취소된 예약이 혼재한다
            예약_생성_요청("A-1", "2026-01-10", "2026-01-12", "홍길동", 4);

            ExtractableResponse<Response> 취소할_예약 = 예약_생성_요청(
                    "A-1", "2026-01-20", "2026-01-22", "김철수", 3);
            Long 예약_id = 취소할_예약.jsonPath().getLong("id");
            String 확인코드 = 취소할_예약.jsonPath().getString("confirmationCode");
            예약_취소_요청(예약_id, 확인코드);

            // when - 관리자가 캘린더를 조회한다
            ExtractableResponse<Response> response = 캘린더_조회_요청(siteId, 2026, 1);

            // then - 확정된 예약만 "예약됨" 상태로 표시된다
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            날짜가_예약됨_검증(response, "2026-01-10", "2026-01-11");

            // and - 취소된 예약은 "이용 가능" 상태로 표시된다
            날짜가_예약가능_검증(response, "2026-01-20", "2026-01-21");
        }

        @Test
        @DisplayName("[정상] 월별 캘린더 전체 조회")
        void 월별_캘린더_전체_조회() {
            // given - 사이트에 예약이 있다
            예약_생성_요청("A-1", "2026-01-15", "2026-01-17", "홍길동", 4);

            // when - 관리자가 월별 캘린더를 조회한다
            ExtractableResponse<Response> response = 캘린더_조회_요청(siteId, 2026, 1);

            // then - 해당 월의 모든 날짜 현황이 반환된다
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            캘린더_기본정보_검증(response, 2026, 1, siteId);
            월별_날짜수_검증(response, JANUARY_DAYS);
        }
    }
}