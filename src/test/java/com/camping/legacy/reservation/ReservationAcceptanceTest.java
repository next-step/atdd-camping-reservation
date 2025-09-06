package com.camping.legacy.reservation;

import com.camping.legacy.utils.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.reservation.ReservationSteps.고객이_예약을_요청한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_가능한_캠핑_사이트_A001이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_상태가_CONFIRMED로_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_성공적으로_생성된다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_실패한다;
import static com.camping.legacy.reservation.ReservationSteps.오늘_날짜가_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.오류_메시지가_반환된다;
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

    // todo: 버그 30일 제한 검증 로직을 추가해야 함
//    @DisplayName("30일 초과 예약 시도")
//    @Test
//    void 예약_30일_초과_시도() {
//        // given
//        오늘_날짜가_설정된다("2024-01-01");
//
//        // when
//        var response = 고객이_예약을_요청한다(
//                "김철수", "010-1234-5678", "2024-02-01", "2024-02-02", "A-1");
//
//        // then
//        예약이_실패한다(response);
//        오류_메시지가_반환된다(response, "30일 이내에만 예약 가능합니다");
//    }
}
