package com.camping.legacy.acceptance.reservation;

import static com.camping.legacy.acceptance.reservation.ReservationCreateAcceptanceTestSteps.예약_생성을_요청한다;
import static com.camping.legacy.acceptance.site.SiteAcceptanceTestSteps.사이트가_존재한다;
import static com.camping.legacy.test.utils.ConcurrencyUtils.runSimultaneously;
import static com.camping.legacy.test.utils.ResponseUtils.isSuccessful;
import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.acceptance.reservation.request.ReservationRequestBuilder;
import com.camping.legacy.test.AcceptanceTest;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ReservationConcurrencyAcceptanceTest extends AcceptanceTest {

    @Test
    void 같은_사이트에_동시에_여러_예약_요청이_들어와도_하나의_예약만_성공한다() throws InterruptedException {
        // given
        var A_1 = 사이트가_존재한다("A-1");

        var 시작일 = LocalDate.now().plusDays(1);
        var 종료일 = 시작일.plusDays(1);

        var 동시_요청_수 = 5;
        var 요청_성공_카운터 = new AtomicInteger(0);
        var 요청_실패_카운터 = new AtomicInteger(0);

        // when
        runSimultaneously(동시_요청_수, () -> {
            var 예약_생성_응답 = 예약_생성을_요청한다(
                new ReservationRequestBuilder()
                    .siteNumber(A_1.getSiteNumber())
                    .startDate(시작일)
                    .endDate(종료일)
                    .build()
            );

            if (isSuccessful(예약_생성_응답)) {
                요청_성공_카운터.incrementAndGet();
            } else {
                요청_실패_카운터.incrementAndGet();
            }
        });

        // then
        assertThat(요청_성공_카운터.get()).isEqualTo(1);
        assertThat(요청_실패_카운터.get()).isEqualTo(동시_요청_수 - 1);
    }
}