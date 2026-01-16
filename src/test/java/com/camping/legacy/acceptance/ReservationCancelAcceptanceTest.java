package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.acceptance.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationCancelAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("올바른 확인코드로 예약 취소 성공")
    void 예약_취소_성공() {
        // Given - 예약 생성
        var createResponse = createReservation("홍길동", "A-1", 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 취소 성공 검증
        var cancelResponse = cancelReservation(reservationId, confirmationCode);

        // Then - 취소 후 상태 확인
        var getResponse = getReservation(reservationId);
        var status = getResponse.jsonPath().getString("status");
        assertThat(status).isIn("CANCELLED", "CANCELLED_SAME_DAY");
    }

    @Test
    @DisplayName("잘못된 확인코드로 취소 시 실패")
    void 잘못된_확인코드_취소_실패() {
        // Given - 예약 생성
        var createResponse = createReservation("홍길동", "A-1", 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");

        // When - 잘못된 확인코드로 취소 요청
        var cancelResponse = cancelReservation(reservationId, "WRONG1");

        // Then - 실패 응답 검증
        assertThat(cancelResponse.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("존재하지 않는 예약 취소 시 실패")
    void 존재하지_않는_예약_취소_실패() {
        // When - 존재하지 않는 예약 취소 요청
        var cancelResponse = cancelReservation(99999L, "ABC123");

        // Then - 실패 응답 검증
        assertThat(cancelResponse.statusCode()).isEqualTo(400);
    }
}
