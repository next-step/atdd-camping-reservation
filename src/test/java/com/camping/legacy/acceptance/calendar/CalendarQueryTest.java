package com.camping.legacy.acceptance.calendar;

import com.camping.legacy.common.AcceptanceTest;
import com.camping.legacy.common.ReservationBuilder.ReservationResult;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.common.CalendarAssertions.*;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캘린더 조회 테스트")
class CalendarQueryTest extends AcceptanceTest {

    private Long siteId;
    private int testYear;
    private int testMonth;
    private int testMonthDays;

    @BeforeEach
    void setUpFixture() {
        siteId = 사이트_ID_조회("A-1");
        testYear = 다음달_연도();
        testMonth = 다음달_월();
        testMonthDays = 다음달_일수();
    }

    @Test
    @DisplayName("월별 캘린더 전체 조회")
    void 월별_캘린더_전체_조회() {
        // given
        예약().사이트("A-1")
                .기간(다음달_일자(15), 다음달_일자(17))
                .고객명("홍길동")
                .생성_요청();

        // when
        ExtractableResponse<Response> response = 캘린더_조회_요청(siteId, testYear, testMonth);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        캘린더_기본정보_검증(response, testYear, testMonth, siteId);
        월별_날짜수_검증(response, testMonthDays);
    }

    @Test
    @DisplayName("확정된 예약만 캘린더에 예약됨으로 표시")
    void 확정_예약만_캘린더_표시() {
        // given - 확정된 예약
        예약().사이트("A-1")
                .기간(다음달_일자(10), 다음달_일자(12))
                .고객명("홍길동")
                .생성_요청();

        // and - 취소된 예약
        ReservationResult 취소할_예약 = 예약()
                .사이트("A-1")
                .기간(다음달_일자(20), 다음달_일자(22))
                .고객명("김철수")
                .생성();

        예약_취소_요청(취소할_예약.ID(), 취소할_예약.확인코드());

        // when
        ExtractableResponse<Response> response = 캘린더_조회_요청(siteId, testYear, testMonth);

        // then - 확정된 예약만 예약됨
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        날짜가_예약됨_검증(response, 다음달_일자(10), 다음달_일자(11));

        // and - 취소된 예약은 이용 가능
        날짜가_예약가능_검증(response, 다음달_일자(20), 다음달_일자(21));
    }
}