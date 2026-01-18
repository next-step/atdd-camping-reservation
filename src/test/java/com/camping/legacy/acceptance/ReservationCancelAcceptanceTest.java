package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.fixture.ReservationFixture;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 취소 인수 테스트")
class ReservationCancelAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        CampsiteFixture.createDefaultSites(campsiteRepository);
    }

    @Nested
    @DisplayName("정상 취소")
    class SuccessCases {

        @Test
        @DisplayName("사전 예약을 정상적으로 취소한다")
        void shouldCancelReservationBeforeStartDate() {
            // given
            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(9);
            ReservationRequest request = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);
            ReservationResponse reservation = ReservationFixture.createReservationAndVerify(request);

            // when
            ExtractableResponse<Response> response = ReservationFixture.cancelReservation(
                    reservation.getId(), reservation.getConfirmationCode());

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            ReservationResponse cancelledReservation = ReservationFixture.getReservation(reservation.getId())
                    .as(ReservationResponse.class);
            assertThat(cancelledReservation.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("당일 예약을 정상적으로 취소한다")
        void shouldCancelSameDayReservation() {
            // given
            LocalDate today = LocalDate.now();
            ReservationRequest request = ReservationFixture.createRequest("홍길동", "A-1", today, today.plusDays(2));
            ReservationResponse reservation = ReservationFixture.createReservationAndVerify(request);

            // when
            ExtractableResponse<Response> response = ReservationFixture.cancelReservation(
                    reservation.getId(), reservation.getConfirmationCode());

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            ReservationResponse cancelledReservation = ReservationFixture.getReservation(reservation.getId())
                    .as(ReservationResponse.class);
            assertThat(cancelledReservation.getStatus()).isEqualTo("CANCELLED_SAME_DAY");
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ErrorCases {

        @Test
        @DisplayName("잘못된 확인코드로 취소 시도 시 실패한다")
        void shouldFailWhenWrongConfirmationCode() {
            // given
            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(9);
            ReservationRequest request = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);
            ReservationResponse reservation = ReservationFixture.createReservationAndVerify(request);

            // when
            ExtractableResponse<Response> response = ReservationFixture.cancelReservation(reservation.getId(), "WRONG1");

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("확인 코드가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 예약 취소 시도 시 실패한다")
        void shouldFailWhenReservationNotExists() {
            // when
            ExtractableResponse<Response> response = ReservationFixture.cancelReservation(9999L, "WRONG1");

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("예약을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("취소 후 재예약")
    class ReservationAfterCancellation {

        @Test
        @Disabled("ISSUE-001: 취소된 예약의 상태를 고려한 중복 체크 로직 구현 필요")
        @DisplayName("취소된 사이트를 다른 고객이 예약할 수 있다")
        void shouldAllowOtherCustomerToReserveAfterCancellation() {
            // given
            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(9);
            ReservationRequest firstRequest = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);
            ReservationResponse firstReservation = ReservationFixture.createReservationAndVerify(firstRequest);

            ReservationFixture.cancelReservation(firstReservation.getId(), firstReservation.getConfirmationCode());

            // when
            ReservationRequest secondRequest = ReservationFixture.createRequest("김철수", "A-1", startDate, endDate);
            ExtractableResponse<Response> response = ReservationFixture.createReservation(secondRequest);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            ReservationResponse secondReservation = response.as(ReservationResponse.class);
            assertThat(secondReservation.getCustomerName()).isEqualTo("김철수");
        }
    }
}
