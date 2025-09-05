package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("동시성 제어")
class ConcurrencyControlAcceptanceTest {

    @Test
    @DisplayName("여러 회원이 동일 사이트를 동일 날짜에 동시에 예약을 할 시 1명만 성공한다")
    void 동시_예약_1명만_성공() {
        // Given 특정 날짜에 예약되지 않은 사이트가 존재한다.

        // When 동시에 동일 사이트 동일 날짜 예약을 수행한다.

        // Then 1명은 성공하고, 나머지는 "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
    }

    @Test
    @DisplayName("여러 회원이 동일 사이트를 취소된 예약이 존재하는 동일 날짜에 동시에 예약을 할 시 1명만 성공한다")
    void 동시_예약_취소된_예약_존재_시_1명만_성공() {
        // Given 특정 날짜의 취소된 예약이 존재하는 사이트가 존재한다.

        // When 동시에 동일 사이트 동일 날짜 예약을 수행한다.

        // Then 1명은 성공하고, 나머지는 "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
    }
}