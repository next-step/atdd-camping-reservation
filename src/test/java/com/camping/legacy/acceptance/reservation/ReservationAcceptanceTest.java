package com.camping.legacy.acceptance.reservation;

import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약_생성을_요청한다;
import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약_생성이_성공한다;
import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약_생성이_실패한다;
import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약이_생성되어있다;
import static com.camping.legacy.acceptance.site.SiteAcceptanceTestSteps.사이트가_존재한다;

import com.camping.legacy.acceptance.reservation.request.ReservationRequestBuilder;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.test.AcceptanceTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReservationAcceptanceTest extends AcceptanceTest {

    private Campsite A_1 = null;

    @BeforeEach
    void setUp() {
        // given
        A_1 = 사이트가_존재한다("A-1");
    }

    @Test
    void 예약_생성_인수테스트() {
        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .build()
        );

        // then
        예약_생성이_성공한다(예약_생성_응답);
    }

    @Test
    void 예약은_오늘로부터_30일_이내에만_가능하다() {
        // given
        var _30일_후 = LocalDate.now().plusDays(30);

        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .startDate(_30일_후.plusDays(1))
                .endDate(_30일_후.plusDays(2))
                .build()
        );

        // then
        예약_생성이_실패한다(예약_생성_응답);
    }

    @Test
    void 과거_날짜로_예약이_불가능하다() {
        // given
        var 어제 = LocalDate.now().minusDays(1);

        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .startDate(어제)
                .endDate(어제.plusDays(1))
                .build()
        );

        // then
        예약_생성이_실패한다(예약_생성_응답);
    }

    @Test
    void 종료일이_시작일보다_이전일_수_없다() {
        // given
        var 내일 = LocalDate.now();
        var 모레 = 내일.plusDays(1);

        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .startDate(모레)
                .endDate(내일)
                .build()
        );

        // then
        예약_생성이_실패한다(예약_생성_응답);
    }

    @Test
    void 예약자_이름이_필수_입력값이다() {
        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .customerName(null)
                .build()
        );

        // then
        예약_생성이_실패한다(예약_생성_응답);
    }

    @Test
    void 예약자_전화번호가_필수_입력값이다() {
        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .phoneNumber(null)
                .build()
        );

        // then
        예약_생성이_실패한다(예약_생성_응답);
    }

    @Test
    void 동일_사이트_동일_기간에_중복_예약이_불가능하다() {
        // given
        var 시작일 = LocalDate.now().plusDays(1);
        var 종료일 = 시작일.plusDays(1);

        예약이_생성되어있다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .startDate(시작일)
                .endDate(종료일)
                .build()
        );

        // when
        var 예약_생성_응답 = 예약_생성을_요청한다(
            new ReservationRequestBuilder()
                .siteNumber("A-1")
                .startDate(시작일)
                .endDate(종료일)
                .build()
        );

        // then
        예약_생성이_실패한다(예약_생성_응답);
    }
}
