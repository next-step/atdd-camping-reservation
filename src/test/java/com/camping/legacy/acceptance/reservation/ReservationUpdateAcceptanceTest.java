package com.camping.legacy.acceptance.reservation;

import static com.camping.legacy.acceptance.reservation.ReservationCreateAcceptanceTestSteps.예약이_생성되어있다;
import static com.camping.legacy.acceptance.reservation.ReservationUpdateAcceptanceTestSteps.예약_수정을_요청한다;
import static com.camping.legacy.acceptance.reservation.ReservationUpdateAcceptanceTestSteps.예약_수정이_성공한다;
import static com.camping.legacy.acceptance.reservation.ReservationUpdateAcceptanceTestSteps.예약_수정이_실패한다;
import static com.camping.legacy.acceptance.site.SiteAcceptanceTestSteps.사이트가_존재한다;

import com.camping.legacy.acceptance.reservation.request.ReservationRequestBuilder;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.test.AcceptanceTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReservationUpdateAcceptanceTest extends AcceptanceTest {

    private Campsite A_1 = null;
    private Campsite A_2 = null;

    private ReservationResponse 기존_예약 = null;

    @BeforeEach
    void setUp() {
        // given
        A_1 = 사이트가_존재한다("A-1");
        A_2 = 사이트가_존재한다("A-2");

        기존_예약 = 예약이_생성되어있다(
            new ReservationRequestBuilder()
                .customerName("테스터")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .siteNumber(A_1.getSiteNumber())
                .phoneNumber("010-1111-2222")
                .build()
        );
    }

    @Test
    void 예약_수정_시_확인_코드가_일치해야한다() {
        // given
        String 잘못된_확인_코드 = 기존_예약.getConfirmationCode() + "X";

        // when
        var 예약_수정_응답 = 예약_수정을_요청한다(
            기존_예약.getId(), 잘못된_확인_코드,
            new ReservationRequestBuilder()
                .siteNumber(A_2.getSiteNumber())
                .build()
        );

        // then
        예약_수정이_실패한다(예약_수정_응답, "확인 코드가 일치하지 않습니다.");
    }

    @Test
    void 사이트_시작일_종료일_예약자_이름_전화번호_수정이_가능하다() {
        // when
        var 예약_수정_응답 = 예약_수정을_요청한다(
            기존_예약.getId(), 기존_예약.getConfirmationCode(),
            new ReservationRequestBuilder()
                .customerName("수정된이름")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(3))
                .siteNumber(A_2.getSiteNumber())
                .phoneNumber("010-4444-5555")
                .build()
        );

        // then
        예약_수정이_성공한다(예약_수정_응답);
    }

    @Test
    void 수정된_예약이_기존_다른_예약과_중복되면_실패한다() {
        // given
        예약이_생성되어있다(
            new ReservationRequestBuilder()
                .customerName("다른예약자")
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(4))
                .siteNumber(A_2.getSiteNumber())
                .phoneNumber("010-3333-4444")
                .build()
        );

        // when
        var 예약_수정_응답 = 예약_수정을_요청한다(
            기존_예약.getId(), 기존_예약.getConfirmationCode(),
            new ReservationRequestBuilder()
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(4))
                .siteNumber(A_2.getSiteNumber())
                .build()
        );

        // then
        예약_수정이_실패한다(예약_수정_응답, "해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 수정하는_날짜는_오늘로부터_30일_이내에만_가능하다() {
        // given
        var _30일_후 = LocalDate.now().plusDays(30);

        // when
        var 예약_수정_응답 = 예약_수정을_요청한다(
            기존_예약.getId(), 기존_예약.getConfirmationCode(),
            new ReservationRequestBuilder()
                .startDate(_30일_후.plusDays(1))
                .endDate(_30일_후.plusDays(2))
                .build()
        );

        // then
        예약_수정이_실패한다(예약_수정_응답, "예약 기간은 오늘로부터 30일 이내에만 가능합니다.");
    }

    @Test
    void 과거_날짜로_예약을_수정할_수_없다() {
        // given
        var 오늘 = LocalDate.now();
        var 어제 = 오늘.minusDays(1);

        // when
        var 예약_수정_응답 = 예약_수정을_요청한다(
            기존_예약.getId(), 기존_예약.getConfirmationCode(),
            new ReservationRequestBuilder()
                .startDate(어제)
                .endDate(오늘)
                .build()
        );

        // then
        예약_수정이_실패한다(예약_수정_응답, "예약 기간은 오늘 이후로 선택해주세요.");
    }

    @Test
    void 종료일이_시작일보다_이전으로_수정될_수_없다() {
        // given
        var 오늘 = LocalDate.now();
        var 내일 = 오늘.plusDays(1);
        var 모레 = 내일.plusDays(1);

        // when
        var 예약_수정_응답 = 예약_수정을_요청한다(
            기존_예약.getId(), 기존_예약.getConfirmationCode(),
            new ReservationRequestBuilder()
                .startDate(모레)
                .endDate(내일)
                .build()
        );

        // then
        예약_수정이_실패한다(예약_수정_응답, "종료일이 시작일보다 이전일 수 없습니다.");
    }
}
