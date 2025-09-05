package com.camping.legacy.acceptance.reservation;

import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약_생성을_요청한다;
import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약_생성이_성공한다;
import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceTestSteps.예약_생성이_실패한다;
import static com.camping.legacy.acceptance.site.SiteAcceptanceTestSteps.사이트가_존재한다;

import com.camping.legacy.acceptance.reservation.request.ReservationRequestBuilder;
import com.camping.legacy.test.AcceptanceTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ReservationAcceptanceTest extends AcceptanceTest {

    @Test
    void 예약_생성_인수테스트() {
        // given
        사이트가_존재한다("A-1");

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
        사이트가_존재한다("A-1");
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
}
