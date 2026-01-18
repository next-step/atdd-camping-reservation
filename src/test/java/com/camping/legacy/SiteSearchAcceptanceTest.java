package com.camping.legacy;

import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.fixture.ReservationRequestBuilder.*;
import static com.camping.legacy.step.ReservationStep.예약을_요청한다;
import static com.camping.legacy.step.SiteStep.기간_조건으로_사이트를_검색한다;
import static com.camping.legacy.step.SiteStep.사이트를_검색한다;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiteSearchAcceptanceTest extends AcceptanceTest {

    @DisplayName("예약이 없는 기간으로 검색 시 가능한 예약 가능한 사이트 목록이 반환된다")
    @Test
    void 예약이_없는_기간으로_검색_시_예약_가능한_사이트_목록이_반환된다() {
        // given
        // A-1, B-1 모두 예약 없는 상태
        var 사이트_검색_결과 = 기간_조건으로_사이트를_검색한다(0, 10);

        // then
        검색_결과에_해당_사이트가_포함된다(사이트_검색_결과, SITE_A1, SITE_B1);
        모든_사이트는_이용가능한_상태이다(사이트_검색_결과);
    }

    @DisplayName("사이트 크기 필터링 및 상세 정보 확인이 가능하다")
    @Test
    void 사이트_크기_필터링_및_상세_정보_확인이_가능하다() {
        // given : "A-1(대형)", "B-1(소형)" 사이트가 등록되어 있다
        var 검색_결과 = 사이트를_검색한다(0, 2, LARGE_SITE_TYPE);

        // then
        검색_결과에_특정_사이트만_포함된다(검색_결과, SITE_A1);
        검색_결과에는_최대인원과_전기가능여부가_포함되야한다(검색_결과);
    }

    @DisplayName("기간 내 중간 날짜가 예약된 사이트는 검색에서 제외된다 (연박 불가)")
    @Test
    void 기간내_중간_날짜가_예약된_사이트는_검색에서_제외된다() {
        // given : A-1 사이트는 이미 예약된 상태, A-2는 예약이 없는 상태
        신규_사이트를_등록한다(SITE_A2, "Large Site", 5);

        var 예약_요청 = aReservationRequest().withStartDate(1).withEndDate(2);
        예약을_요청한다(예약_요청.build());

        // when & then
        var 검색_결과 = 사이트를_검색한다(0, 3, LARGE_SITE_TYPE);

        검색_결과에_특정_사이트는_포함되지_않는다(검색_결과, SITE_A1);
        검색_결과에_해당_사이트가_포함된다(검색_결과, SITE_A2);
    }

    @DisplayName("과거 날짜 포함하여 검색할 경우 요청이 거부된다")
    @Test
    void 과거_날짜_포함하여_검색할_경우_요청이_거부된다() {
        // given
        // when
        var 사이트_검색_결과 = 기간_조건으로_사이트를_검색한다(-1, 1);

        // then
        검색_요청이_거부되었다(사이트_검색_결과);
    }
}
