package com.camping.legacy.service;

import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.camping.legacy.builder.ReservationRequestBuilder.aReservation;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("예약 서비스 단위 테스트")
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
        CampsiteFixture.기본_사이트_생성(campsiteRepository);
    }

    @Nested
    @DisplayName("날짜 검증")
    class 날짜_검증 {

        @Test
        @DisplayName("과거 날짜로 예약 시도 시 예외가 발생한다")
        void 과거_날짜로_예약하면_예외가_발생한다() {
            // given
            var request = aReservation()
                    .pastDate(3)
                    .build();

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("과거 날짜로 예약할 수 없습니다.");
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전이면 예외가 발생한다")
        void 종료일이_시작일보다_이전이면_예외가_발생한다() {
            // given
            var 시작일 = java.time.LocalDate.now().plusDays(10);
            var 종료일 = java.time.LocalDate.now().plusDays(7);
            var request = aReservation()
                    .startDate(시작일)
                    .endDate(종료일)
                    .build();

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("종료일이 시작일보다 이전일 수 없습니다.");
        }

        @Test
        @DisplayName("30일 초과 기간 예약 시 예외가 발생한다")
        void 삼십일_초과_기간_예약시_예외가_발생한다() {
            // given
            var 시작일 = java.time.LocalDate.now().plusDays(7);
            var request = aReservation()
                    .startDate(시작일)
                    .endDate(시작일.plusDays(40))
                    .build();

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("예약 기간은 최대 30일입니다.");
        }
    }

    @Nested
    @DisplayName("고객 정보 검증")
    class 고객_정보_검증 {

        @Test
        @DisplayName("이름이 2자 미만이면 예외가 발생한다")
        void 이름이_이자_미만이면_예외가_발생한다() {
            // given
            var request = aReservation()
                    .customerName("김")
                    .build();

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("예약자 이름은 최소 2자 이상이어야 합니다.");
        }

        @Test
        @DisplayName("이름이 비어있으면 예외가 발생한다")
        void 이름이_비어있으면_예외가_발생한다() {
            // given
            var request = aReservation()
                    .customerName("")
                    .build();

            // when & then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("예약자 이름을 입력해주세요.");
        }
    }
}
