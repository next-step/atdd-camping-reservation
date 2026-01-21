package com.camping.legacy.unit.calendar;

import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("캘린더 서비스 단위 테스트")
class CalendarServiceTest {

    @Mock
    private CampsiteRepository campsiteRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Nested
    @DisplayName("월별 캘린더 조회 검증")
    class GetMonthlyCalendarValidation {

        @Test
        @DisplayName("존재하지 않는 사이트 조회시 예외 발생")
        void 존재하지_않는_사이트_예외() {
            // given
            given(campsiteRepository.findById(99999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.getMonthlyCalendar(2026, 1, 99999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }
}