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

@DisplayName("예약 생성 인수 테스트")
class ReservationCreateAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        CampsiteFixture.createDefaultSites(campsiteRepository);
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("정상적인 예약 생성")
        void shouldCreateReservationSuccessfully() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(9);
            var request = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            var reservation = response.as(ReservationResponse.class);
            assertThat(reservation.getConfirmationCode()).hasSize(6);
            assertThat(reservation.getStatus()).isEqualTo("CONFIRMED");
            assertThat(reservation.getCustomerName()).isEqualTo("홍길동");
            assertThat(reservation.getSiteNumber()).isEqualTo("A-1");
        }

        @Test
        @DisplayName("당일 예약 생성")
        void shouldCreateSameDayReservationSuccessfully() {
            // given
            var today = LocalDate.now();
            var request = ReservationFixture.createRequest("홍길동", "A-1", today, today);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            var reservation = response.as(ReservationResponse.class);
            assertThat(reservation.getStartDate()).isEqualTo(today);
            assertThat(reservation.getEndDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("취소된 예약 기간에 재예약")
         void shouldAllowReservationAfterCancellation() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(9);
            var firstRequest = ReservationFixture.createRequest("김철수", "A-1", startDate, endDate);
            var firstReservation = ReservationFixture.createReservationAndVerify(firstRequest);

            ReservationFixture.cancelReservation(firstReservation.getId(), firstReservation.getConfirmationCode());

            // when
            var secondRequest = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);
            var response = ReservationFixture.createReservation(secondRequest);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        }
    }

    @Nested
    @DisplayName("날짜 검증 예외")
    class DateValidationErrors {

        @Test
        @DisplayName("과거 날짜로 예약 시도 시 실패한다")
        void shouldFailWhenReservingWithPastDate() {
            // given
            var pastDate = LocalDate.now().minusDays(3);
            var request = ReservationFixture.createRequest("홍길동", "A-1", pastDate, pastDate.plusDays(2));

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("과거 날짜로 예약할 수 없습니다.");
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전이면 실패한다")
        void shouldFailWhenEndDateBeforeStartDate() {
            // given
            var startDate = LocalDate.now().plusDays(10);
            var endDate = LocalDate.now().plusDays(7);
            var request = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
        }

        @Test
        @DisplayName("30일 초과 기간 예약 시 실패한다")
        void shouldFailWhenExceedingMaxDays() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = startDate.plusDays(40);
            var request = ReservationFixture.createRequest("홍길동", "A-1", startDate, endDate);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("예약 기간은 최대 30일입니다.");
        }
    }

    @Nested
    @DisplayName("고객 정보 검증 예외")
    class CustomerValidationErrors {

        @Test
        @DisplayName("이름이 2자 미만이면 실패한다")
        void shouldFailWhenNameTooShort() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(9);
            var request = ReservationFixture.createRequest("김", "A-1", startDate, endDate);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("예약자 이름은 최소 2자 이상이어야 합니다.");
        }

        @Test
        @DisplayName("이름이 비어있으면 실패한다")
        void shouldFailWhenNameEmpty() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(9);
            var request = ReservationFixture.createRequest("", "A-1", startDate, endDate);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("예약자 이름을 입력해주세요.");
        }
    }

    @Nested
    @DisplayName("사이트 검증 예외")
    class SiteValidationErrors {

        @Test
        @DisplayName("존재하지 않는 사이트 예약 시 실패한다")
        void shouldFailWhenSiteNotExists() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(9);
            var request = ReservationFixture.createRequest("홍길동", "Z-999", startDate, endDate);

            // when
            var response = ReservationFixture.createReservation(request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("존재하지 않는 캠핑장입니다.");
        }
    }

    @Nested
    @DisplayName("중복 예약 예외")
    class DuplicateReservationErrors {

        @Test
        @DisplayName("동일 기간 중복 예약 시 실패한다")
        void shouldFailWhenDuplicatePeriod() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(9);
            var firstRequest = ReservationFixture.createRequest("김철수", "A-1", startDate, endDate);
            ReservationFixture.createReservation(firstRequest);

            // when
            var secondRequest = ReservationFixture.createRequest("홍길동", "A-1", startDate.plusDays(1), endDate.plusDays(1));
            var response = ReservationFixture.createReservation(secondRequest);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
        }
    }
}
