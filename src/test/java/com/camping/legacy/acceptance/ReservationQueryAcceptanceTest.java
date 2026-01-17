package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.acceptance.ReservationSteps.createReservation;
import static com.camping.legacy.acceptance.ReservationSteps.getReservation;
import static org.assertj.core.api.Assertions.assertThat;
import static com.camping.legacy.acceptance.TestFixture.*;

public class ReservationQueryAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("예약 단건 조회 성공")
    void 예약_단건_조회_성공() {
        // Given - 예약 생성
        var createResponse = createReservation(DEFAULT_CUSTOMER, SITE_A1, 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");

        // When - 예약 조회
        var getResponse = getReservation(reservationId);

        // Then - 조회 성공 검증
        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getString("customerName")).isEqualTo(DEFAULT_CUSTOMER);
        assertThat(getResponse.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
    }

    @Test
    @DisplayName("존재하지 않는 예약 조회 시 404")
    void 존재하지_않는_예약_조회_실패() {
        // When - 존재하지 않는 예약 조회
        var getResponse = getReservation(99999L);

        // Then - 404 응답 검증
        assertThat(getResponse.statusCode()).isEqualTo(404);
    }
}
