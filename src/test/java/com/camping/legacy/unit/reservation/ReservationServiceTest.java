package com.camping.legacy.unit.reservation;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CampsiteRepository campsiteRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Campsite testCampsite;

    @BeforeEach
    void setUp() {
        testCampsite = new Campsite();
        testCampsite.setId(1L);
        testCampsite.setSiteNumber("A-1");
        testCampsite.setMaxPeople(6);
    }

    @Nested
    @DisplayName("예약 생성 - 입력값 검증")
    class CreateReservationValidation {

        @Test
        @DisplayName("사이트 번호가 null이면 예외 발생")
        void 사이트번호_null_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber(null);

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("사이트 번호");
        }

        @Test
        @DisplayName("사이트 번호가 빈 문자열이면 예외 발생")
        void 사이트번호_빈문자열_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("");

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("사이트 번호");
        }

        @Test
        @DisplayName("존재하지 않는 사이트면 예외 발생")
        void 존재하지_않는_사이트_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("Z-999");

            given(campsiteRepository.findBySiteNumberWithLock("Z-999"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("존재하지 않는");
        }

        @Test
        @DisplayName("시작일이 null이면 예외 발생")
        void 시작일_null_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(null);
            request.setEndDate(LocalDate.now().plusDays(5));

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("기간");
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전이면 예외 발생")
        void 종료일이_시작일보다_이전_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(5));

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("종료일");
        }

        @Test
        @DisplayName("과거 날짜로 예약하면 예외 발생")
        void 과거_날짜_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.of(2020, 1, 1));
            request.setEndDate(LocalDate.of(2020, 1, 3));

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("과거");
        }
    }

    @Nested
    @DisplayName("예약 생성 - 인원수 검증")
    class CreateReservationPeopleValidation {

        @Test
        @DisplayName("인원이 0명이면 예외 발생")
        void 인원_0명_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(12));
            request.setNumberOfPeople(0);

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("인원");
        }

        @Test
        @DisplayName("최대 인원 초과시 예외 발생")
        void 최대_인원_초과_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(12));
            request.setNumberOfPeople(10); // 최대 6명

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("최대 인원");
        }
    }

    @Nested
    @DisplayName("예약 생성 - 고객 정보 검증")
    class CreateReservationCustomerValidation {

        @Test
        @DisplayName("고객명이 null이면 예외 발생")
        void 고객명_null_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(12));
            request.setNumberOfPeople(4);
            request.setCustomerName(null);

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이름");
        }

        @Test
        @DisplayName("고객명이 빈 문자열이면 예외 발생")
        void 고객명_빈문자열_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(12));
            request.setNumberOfPeople(4);
            request.setCustomerName("");

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이름");
        }

        @Test
        @DisplayName("고객명이 2자 미만이면 예외 발생")
        void 고객명_2자_미만_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setSiteNumber("A-1");
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(12));
            request.setNumberOfPeople(4);
            request.setCustomerName("김");

            given(campsiteRepository.findBySiteNumberWithLock("A-1"))
                    .willReturn(Optional.of(testCampsite));

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("2자");
        }
    }

    @Nested
    @DisplayName("예약 취소 검증")
    class CancelReservationValidation {

        @Test
        @DisplayName("존재하지 않는 예약 취소시 예외 발생")
        void 존재하지_않는_예약_취소_예외() {
            // given
            given(reservationRepository.findById(99999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(99999L, "ABC123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("예약 수정 검증")
    class UpdateReservationValidation {

        @Test
        @DisplayName("존재하지 않는 예약 수정시 예외 발생")
        void 존재하지_않는_예약_수정_예외() {
            // given
            ReservationRequest request = new ReservationRequest();
            request.setStartDate(LocalDate.now().plusDays(10));
            request.setEndDate(LocalDate.now().plusDays(12));

            given(reservationRepository.findById(99999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.updateReservation(99999L, request, "ABC123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }
}