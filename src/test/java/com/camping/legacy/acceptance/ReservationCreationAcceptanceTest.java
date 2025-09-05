package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("예약 생성")
class ReservationCreationAcceptanceTest {

    @Test
    @DisplayName("예약을 한 날짜가 예약한 날짜와 30일 이내 차이가 나면 예약이 성공한다")
    void 예약_성공_30일_이내() {
        // Given 특정 날짜와 오늘이 30일 이내 차이 나는 사이트가 존재한다 (+ 모든 값이 채워진).

        // When 회원이 특정 날짜의 예약을 수행한다.

        // Then 6자리 영숫자 확인 코드가 생성된다.
        // And 예약이 생성된다(예약 상태가 "CONFIRMED").
    }

    @Test
    @DisplayName("예약을 한 날짜가 예약한 날짜와 30일 초과 차이가 나면 예외가 발생한다")
    void 예약_실패_30일_초과() {
        // Given 특정 날짜와 오늘이 30일 초과 차이 나는 사이트가 존재한다.

        // When 회원이 특정 날짜의 예약을 수행한다.

        // Then "예약은 최대 30일 전까지만 가능합니다" 오류 메시지가 반환된다.
    }

    @Test
    @DisplayName("이미 예약된 사이트 예약을 한 경우 예외가 발생한다")
    void 예약_실패_이미_예약된_사이트() {
        // Given 특정 날짜가 예약된 사이트가 존재한다.

        // When 회원이 특정 날짜의 예약을 수행한다.

        // Then "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
    }
}