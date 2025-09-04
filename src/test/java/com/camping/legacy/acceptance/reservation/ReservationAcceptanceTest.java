package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTest;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.reservation.ReservationAcceptanceStep.예약_생성_성공;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationAcceptanceTest extends AcceptanceTest {

    @DisplayName("예약 생성 - 성공")
    @Test
    void createReservation() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "A-1",
                "010-1234-5678",
                2,
                null,
                null
        );

        ReservationResponse response = 예약_생성_성공(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo(request.getCustomerName());
        assertThat(response.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
        assertThat(response.getSiteNumber()).isEqualTo(request.getSiteNumber());
        assertThat(response.getConfirmationCode()).isNotNull();
    }
}
