package com.camping.legacy;

import static com.camping.legacy.assertion.SiteAssertion.*;
import static com.camping.legacy.client.SiteClient.*;
import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.fixture.ReservationRequestBuilder.*;
import static com.camping.legacy.step.ReservationStep.*;
import static com.camping.legacy.step.ReservationStep.예약을_완료한다;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiteSearchAcceptanceTest extends AcceptanceTest {

    @DisplayName("예약이 없는 기간으로 검색 시 가능한 예약 가능한 사이트 목록이 반환된다")
    @Test
    void 예약이_없는_기간으로_검색_시_예약_가능한_사이트_목록이_반환된다() {
        // given : A-1, B-1 모두 예약 없는 상태
        var 응답 = 기간_조건으로_사이트를_검색한다(0, 10);

        // then
        검색_결과에_해당_사이트가_포함된다(응답, SITE_A1, SITE_B1);
        모든_사이트는_이용가능한_상태이다(응답);
    }

    @DisplayName("사이트 크기 필터링 및 상세 정보 확인이 가능하다")
    @Test
    void 사이트_크기_필터링_및_상세_정보_확인이_가능하다() {
        // given : "A-1(대형)", "B-1(소형)" 사이트가 등록되어 있다
        var 응답 = 사이트를_검색한다(0, 2, LARGE_SITE_TYPE);

        // then
        검색_결과에_특정_사이트_하나만_포함된다(응답, SITE_A1);
        검색_결과에_최대인원_정보가_있다(응답);
        검색_결과에_전기가능여부_정보가_있다(응답);
    }

    @DisplayName("기간 내 중간 날짜가 예약된 사이트는 검색에서 제외된다 (연박 불가)")
    @Test
    void 기간내_중간_날짜가_예약된_사이트는_검색에서_제외된다() {
        // given : A-1는 이미 예약된 상태, A-2는 예약이 없는 상태
        예약을_완료한다(aReservationRequest().withSiteNumber(SITE_A1).withStartDate(1).withEndDate(2));
        신규_사이트를_등록한다(SITE_A2, "Large Site", 5);

        // when
        var 응답 = 사이트를_검색한다(0, 3, LARGE_SITE_TYPE);

        // then
        검색_결과에_특정_사이트는_포함되지_않는다(응답, SITE_A1);
        검색_결과에_해당_사이트가_포함된다(응답, SITE_A2);
    }

    @DisplayName("과거 날짜 포함하여 검색할 경우 요청이 거부된다")
    @Test
    void 과거_날짜_포함하여_검색할_경우_요청이_거부된다() {
        // given
        // when
        var 응답 = 기간_조건으로_사이트를_검색한다(-1, 1);

        // then
        검색_요청이_거부되었다(응답);
    }

    @DisplayName("경계값: 기존 예약 종료일이 검색 시작일과 같으면 해당 사이트는 검색된다")
    @Test
    void 기존_예약_종료일과_검색_시작일이_같으면_검색된다() {
        // given
        예약을_완료한다(aReservationRequest()
                .withStartDate(2)
                .withEndDate(4)
                .withSiteNumber(SITE_A1));

        // when
        var 응답 = 사이트를_검색한다(4, 6, LARGE_SITE_TYPE);

        // then
        검색_결과에_해당_사이트가_포함된다(응답, SITE_A1);
    }

    @DisplayName("경계값: 기존 예약 시작일이 검색 종료일과 같으면 해당 사이트는 검색된다")
    @Test
    void 기존_예약_시작일과_검색_종료일이_같으면_검색된다() {
        // given;
        예약을_완료한다(aReservationRequest()
                .withStartDate(2)
                .withEndDate(4)
                .withSiteNumber(SITE_A1));

        // when
        var 응답 = 사이트를_검색한다(0, 2, LARGE_SITE_TYPE);

        // then
        검색_결과에_해당_사이트가_포함된다(응답, SITE_A1);
    }

    @DisplayName("엣지: 취소된 예약 기간은 다시 검색되었을 때 예약 가능한 상태로 노출된다")
    @Test
    void 취소된_예약_기간은_검색_시_예약_가능해야_한다() {
        // given
        사전_예약을_취소한다(aReservationRequest()
                .withStartDate(3)
                .withEndDate(5)
                .withSiteNumber(SITE_A1));

        // when
        var 응답 = 사이트를_검색한다(3, 5, LARGE_SITE_TYPE);

        // then
        검색_결과에_해당_사이트가_포함된다(응답, SITE_A1);
    }
}
