package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.camping.legacy.client.ReservationClient;
import com.camping.legacy.client.SiteClient;

import static com.camping.legacy.steps.ReservationSteps.*;
import static com.camping.legacy.steps.SiteSteps.*;

@DisplayName("사이트 가용성 확인 인수 테스트")
class SiteAvailabilityAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void 사전_데이터_준비() {
        CampsiteFixture.전체_사이트_생성(campsiteRepository);
    }

    @Nested
    @DisplayName("단일 날짜 조회")
    class 단일_날짜_조회 {

        @Test
        @DisplayName("예약된 사이트는 가용 목록에서 제외된다")
        void 예약된_사이트는_가용_목록에서_제외된다() {
            // given
            var 조회날짜 = 일_후(7);
            예약_생성됨("김철수", "A-1", 조회날짜, 조회날짜.plusDays(2));

            // when
            var 가용_사이트_목록 = 가용_사이트_조회(조회날짜);

            // then
            가용_목록에_포함됨(가용_사이트_목록, "A-2");
            가용_목록에서_제외됨(가용_사이트_목록, "A-1");
        }
    }

    @Nested
    @DisplayName("기간 검색")
    class 기간_검색 {

        @Test
        @DisplayName("기간 내 예약이 있는 사이트는 검색 결과에서 제외된다")
        void 기간_내_예약이_있는_사이트는_제외된다() {
            // given
            var 시작일 = 일_후(7);
            var 종료일 = 일_후(10);
            예약_생성됨("김철수", "A-1", 시작일.plusDays(1), 시작일.plusDays(2));

            // when
            var 가용_사이트_목록 = 기간별_가용_사이트_검색(시작일, 종료일);

            // then
            가용_목록에서_제외됨(가용_사이트_목록, "A-1");
        }
    }

    @Nested
    @DisplayName("취소 반영")
    class 취소_반영 {

        @Test
        @DisplayName("취소된 예약의 사이트는 가용 목록에 표시된다")
        void 취소된_예약의_사이트는_가용_목록에_표시된다() {
            // given
            var 조회날짜 = 일_후(7);
            var 예약 = 예약_생성됨("김철수", "A-1", 조회날짜, 조회날짜.plusDays(2));
            ReservationClient.예약_취소_API(예약.getId(), 예약.getConfirmationCode());

            // when
            var 가용_사이트_목록 = 가용_사이트_조회(조회날짜);

            // then
            가용_목록에_포함됨(가용_사이트_목록, "A-1");
        }
    }

    @Nested
    @DisplayName("특정 사이트 가용성 확인")
    class 특정_사이트_가용성_확인 {

        @Test
        @DisplayName("예약 가능한 사이트 조회 시 available이 true이다")
        void 예약_가능한_사이트는_가용함으로_표시된다() {
            // given
            var 조회날짜 = 일_후(7);

            // when
            var 가용여부 = 사이트_가용_여부_확인("A-1", 조회날짜);

            // then
            사이트가_예약_가능함(가용여부);
        }

        @Test
        @DisplayName("예약된 사이트 조회 시 available이 false이다")
        void 예약된_사이트는_가용불가로_표시된다() {
            // given
            var 조회날짜 = 일_후(7);
            예약_생성됨("김철수", "A-1", 조회날짜, 조회날짜.plusDays(2));

            // when
            var 가용여부 = 사이트_가용_여부_확인("A-1", 조회날짜);

            // then
            사이트가_예약_불가능함(가용여부);
        }
    }

    @Nested
    @DisplayName("예외")
    class 예외_케이스 {

        @Test
        @DisplayName("과거 날짜로 가용성 조회 시 에러가 발생한다")
        void 과거_날짜로_가용성_조회시_에러가_발생한다() {
            // given
            var 과거날짜 = 일_전(3);

            // when
            var 응답 = SiteClient.사이트_가용성_확인_API("A-1", 과거날짜);

            // then
            응답_실패_확인(응답, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
