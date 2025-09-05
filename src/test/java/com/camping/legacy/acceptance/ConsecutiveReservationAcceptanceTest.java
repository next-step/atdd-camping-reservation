package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("연박 예약")
class ConsecutiveReservationAcceptanceTest {

    @Test
    @DisplayName("시작일과 종료일을 지정한 특정 기간에 연박 예약을 할 수 있다")
    void 연박_예약_성공() {
        // Given 연박 기간동안 예약되지 않은 사이트가 존재한다.

        // When 회원이 연박 기간동안 예약을 한다.

        // Then 예약이 생성된다.
    }

    @Test
    @DisplayName("특정 기간에 연박 예약 시 취소된 예약과 예약이 없는 경우에도 연박 예약을 할 수 있다")
    void 연박_예약_취소된_예약_포함_성공() {
        // Given 연박 기간동안 취소된 예약과 예약되지 않은 사이트가 존재한다.

        // When 회원이 연박 기간동안 예약을 한다.

        // Then 예약이 생성된다.
    }

    @Test
    @DisplayName("연박 예약 중 연박 기간에 취소되지 않은 예약이 존재하면 예외가 발생한다")
    void 연박_예약_실패_기존_예약_존재() {
        // Given 연박 기간동안 일부 or 전체 예약된 사이트가 존재한다.

        // When 회원이 연박 기간동안 예약을 한다.

        // Then "선택한 기간에 이미 예약된 날짜가 있습니다" 오류 메시지가 반환된다.
    }
}