package com.camping.legacy.acceptance.calendar;

import com.camping.legacy.common.AcceptanceTest;
import com.camping.legacy.common.ReservationBuilder.ReservationResult;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.common.CalendarAssertions.날짜가_예약가능_검증;
import static com.camping.legacy.common.ReservationAssertions.예약_생성_성공_검증;
import static com.camping.legacy.common.ReservationBuilder.예약;
import static com.camping.legacy.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캘린더 - 취소된 예약 처리 테스트")
class CalendarCancelledReservationTest extends AcceptanceTest {

    private Long siteId;
    private int testYear;
    private int testMonth;

    @BeforeEach
    void setUpFixture() {
        siteId = 사이트_ID_조회("A-1");
        testYear = 다음달_연도();
        testMonth = 다음달_월();
    }

    @Test
    @DisplayName("취소된 예약은 캘린더에서 이용 가능으로 표시")
    void 취소된_예약_이용가능_표시() {
        // given
        ReservationResult 취소할_예약 = 예약()
                .사이트("A-1")
                .기간(다음달_일자(20), 다음달_일자(22))
                .고객명("홍길동")
                .생성();

        예약_취소_요청(취소할_예약.ID(), 취소할_예약.확인코드());

        // when
        ExtractableResponse<Response> response = 캘린더_조회_요청(siteId, testYear, testMonth);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        날짜가_예약가능_검증(response, 다음달_일자(20), 다음달_일자(21));
    }

    @Test
    @DisplayName("취소된 날짜에 새로운 예약 가능")
    void 취소된_날짜_재예약_성공() {
        // given
        ReservationResult 취소할_예약 = 예약()
                .사이트("A-1")
                .기간(다음달_일자(20), 다음달_일자(22))
                .고객명("홍길동")
                .생성();

        예약_취소_요청(취소할_예약.ID(), 취소할_예약.확인코드());

        // when
        ExtractableResponse<Response> 재예약_응답 = 예약()
                .사이트("A-1")
                .기간(다음달_일자(20), 다음달_일자(22))
                .고객명("김철수")
                .생성_요청();

        // then
        예약_생성_성공_검증(재예약_응답, "김철수");
    }
}