package com.camping.legacy.reservation;

import com.camping.legacy.utils.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.reservation.ReservationSteps.고객이_예약을_요청한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_가능한_캠핑_사이트_A001이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_상태가_CONFIRMED로_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_성공적으로_생성된다;
import static com.camping.legacy.reservation.ReservationSteps.오늘_날짜가_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.확인코드_6자리가_생성된다;

public class ReservationAcceptanceTest extends AcceptanceTest {

    @DisplayName("정상적인 예약을 생성한다.")
    @Test
    void 정상적인_예약_생성() {
        // given
        예약_가능한_캠핑_사이트_A001이_존재한다();
        오늘_날짜가_설정된다("2024-01-01");

        // when
        var response = 고객이_예약을_요청한다(
                "김철수", "010-1234-5678", "2024-01-15", "2024-01-16", "A-1");

        // then
        예약이_성공적으로_생성된다(response);
        확인코드_6자리가_생성된다(response);
        예약_상태가_CONFIRMED로_설정된다(response);
    }
}
