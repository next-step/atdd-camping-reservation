package com.camping.legacy.acceptance.reservation;

import static com.camping.legacy.acceptance.reservation.ReservationCreateAcceptanceTestSteps.예약이_생성되어있다;
import static com.camping.legacy.acceptance.reservation.ReservationUpdateAcceptanceTestSteps.예약_수정을_요청한다;
import static com.camping.legacy.acceptance.reservation.ReservationUpdateAcceptanceTestSteps.예약_수정이_실패한다;
import static com.camping.legacy.acceptance.site.SiteAcceptanceTestSteps.사이트가_존재한다;

import com.camping.legacy.acceptance.reservation.request.ReservationRequestBuilder;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.test.AcceptanceTest;
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
                .siteNumber(A_1.getSiteNumber())
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
}
