package com.camping.legacy.reservation;

import com.camping.legacy.utils.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.reservation.ReservationSteps.고객이_30일_초과_연박_예약을_요청한다;
import static com.camping.legacy.reservation.ReservationSteps.고객이_연박_예약을_요청한다;
import static com.camping.legacy.reservation.ReservationSteps.사이트가_기간동안_예약_가능하다;
import static com.camping.legacy.reservation.ReservationSteps.사이트가_날짜에_예약_가능하다;
import static com.camping.legacy.reservation.ReservationSteps.사이트가_날짜에_이미_예약되어_있다;
import static com.camping.legacy.reservation.ReservationSteps.연박_예약이_성공적으로_생성된다;
import static com.camping.legacy.reservation.ReservationSteps.연박_예약이_실패한다;
import static com.camping.legacy.reservation.ReservationSteps.오늘_날짜가_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.오류_메시지가_반환된다;
import static com.camping.legacy.reservation.ReservationSteps.전체_기간에_대한_예약이_생성된다;

public class ConsecutiveReservationAcceptanceTest extends AcceptanceTest {

    @DisplayName("정상적인 연박 예약을 생성한다.")
    @Test
    void 정상적인_연박_예약() {
        // given
        사이트가_기간동안_예약_가능하다("A-1", "2024-01-15", "2024-01-17");

        // when
        var response = 고객이_연박_예약을_요청한다(
                "최영수", "010-4444-4444", "2024-01-15", "2024-01-17", "A-1");

        // then
        연박_예약이_성공적으로_생성된다(response);
        전체_기간에_대한_예약이_생성된다(response, "2024-01-15", "2024-01-17");
    }

    @DisplayName("연박 기간 중 일부 날짜 예약 불가 시 실패한다.")
    @Test
    void 연박_기간_중_일부_날짜_예약_불가() {
        // given
        사이트가_날짜에_예약_가능하다("A-1", "2024-01-15");
        사이트가_날짜에_이미_예약되어_있다("A-1", "2024-01-16");
        사이트가_날짜에_예약_가능하다("A-1", "2024-01-17");

        // when
        var response = 고객이_연박_예약을_요청한다(
                "정민호", "010-5555-5555", "2024-01-15", "2024-01-17", "A-1");

        // then
        연박_예약이_실패한다(response);
        오류_메시지가_반환된다(response, "해당 기간에 이미 예약이 존재합니다.");
    }

    @DisplayName("30일 초과 연박 예약은 실패한다.")
    @Test
    void 연박_예약_30일_제한_확인() {
        // given
        오늘_날짜가_설정된다("2024-01-01");

        // when
        var response = 고객이_30일_초과_연박_예약을_요청한다(
                "한지민", "010-6666-6666", "2024-01-30", "2024-02-05", "A-1");

        // then
        연박_예약이_실패한다(response);
        오류_메시지가_반환된다(response, "30일 이내에만 예약 가능합니다.");
    }
}
