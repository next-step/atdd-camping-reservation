package com.camping.legacy.service;

import com.camping.legacy.dto.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feature: 예약 포인트 적립")
class ReservationPointsTest {

    private PricingCalculator pricingCalculator;

    @BeforeEach
    void setUp() {
        pricingCalculator = new PricingCalculator();
    }

    // =============================================
    // 기본 적립률 (5%) 테스트
    // =============================================

    @Nested
    @DisplayName("기본 적립률 (5%) - 평일만 포함")
    class DefaultPointRateTest {

        @Test
        @DisplayName("평일 3일 A 사이트 - 240,000원의 5% = 12,000P")
        void weekdayOnlyPoints() {
            // 2030-02-04(월) ~ 02-06(수) 평일 3일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 6));

            assertThat(result.getTotalPrice()).isEqualTo(240000);
            assertThat(result.getEarnedPoints()).isEqualTo(12000);
        }

        @Test
        @DisplayName("성수기 평일 B 사이트 - 75,000원의 5% = 3,750P (성수기는 적립률 무관)")
        void peakSeasonWeekdayPoints() {
            // 2030-07-01(월) 성수기 평일 1일
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 7, 1), LocalDate.of(2030, 7, 1));

            assertThat(result.getTotalPrice()).isEqualTo(75000);
            assertThat(result.getEarnedPoints()).isEqualTo(3750);
        }

        @Test
        @DisplayName("금요일만 예약 - 주말 미포함이므로 5% 적립")
        void fridayOnlyIsNotWeekend() {
            // 2030-02-01 = 금요일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 1));

            assertThat(result.getTotalPrice()).isEqualTo(80000);
            assertThat(result.getEarnedPoints()).isEqualTo(4000);
        }

        @Test
        @DisplayName("기타 사이트 평일 1일 - 60,000원의 5% = 3,000P")
        void cSiteWeekdayPoints() {
            // 2030-02-04 = 월요일
            PricingResult result = pricingCalculator.calculate("C-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 4));

            assertThat(result.getTotalPrice()).isEqualTo(60000);
            assertThat(result.getEarnedPoints()).isEqualTo(3000);
        }
    }

    // =============================================
    // 주말 포함 적립률 (10%) 테스트
    // =============================================

    @Nested
    @DisplayName("주말 포함 적립률 (10%)")
    class WeekendPointRateTest {

        @Test
        @DisplayName("토요일 1일만 예약 - 104,000원의 10% = 10,400P")
        void saturdayOnlyPoints() {
            // 2030-02-02 = 토요일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 2), LocalDate.of(2030, 2, 2));

            assertThat(result.getTotalPrice()).isEqualTo(104000);
            assertThat(result.getEarnedPoints()).isEqualTo(10400);
        }

        @Test
        @DisplayName("금~토(평일1 + 주말1) - 184,000원의 10% = 18,400P")
        void weekdayAndWeekendMixPoints() {
            // 2030-02-01(금)=80000, 02-02(토)=104000 → 합계 184000
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 2));

            assertThat(result.getTotalPrice()).isEqualTo(184000);
            assertThat(result.getEarnedPoints()).isEqualTo(18400);
        }

        @Test
        @DisplayName("토~일 B 사이트 - 130,000원의 10% = 13,000P")
        void weekendTwoDaysPoints() {
            // 2030-02-02(토)=65000, 02-03(일)=65000 → 합계 130000
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 2, 2), LocalDate.of(2030, 2, 3));

            assertThat(result.getTotalPrice()).isEqualTo(130000);
            assertThat(result.getEarnedPoints()).isEqualTo(13000);
        }

        @Test
        @DisplayName("성수기 주말 포함 - 할증 가격 기준으로 10% 적립")
        void peakWeekendPoints() {
            // 2030-07-05(금)=성수기 평일 120000 + 07-06(토)=성수기 주말 136000
            // 합계 256000 × 10% = 25600P
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 7, 5), LocalDate.of(2030, 7, 6));

            assertThat(result.getTotalPrice()).isEqualTo(256000);
            assertThat(result.getEarnedPoints()).isEqualTo(25600);
        }
    }

    // =============================================
    // 경계값 테스트
    // =============================================

    @Nested
    @DisplayName("포인트 경계값")
    class PointsBoundaryTest {

        @Test
        @DisplayName("금요일 vs 토요일 경계 - 금요일은 5%, 토요일은 10%")
        void fridayVsSaturdayBoundary() {
            // 금요일 (2030-02-01) - 5%
            PricingResult fridayResult = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 1));
            assertThat(fridayResult.getEarnedPoints()).isEqualTo(4000); // 80000 × 5%

            // 토요일 (2030-02-02) - 10%
            PricingResult saturdayResult = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 2, 2), LocalDate.of(2030, 2, 2));
            assertThat(saturdayResult.getEarnedPoints()).isEqualTo(6500); // 65000 × 10%
        }

        @Test
        @DisplayName("포인트는 총액 기준 단일 비율 적용 (일별이 아님)")
        void pointsAppliedToTotalNotPerDay() {
            // 2030-02-01(금) ~ 02-03(일): 금=80000, 토=104000, 일=104000
            // 총 288000 × 10% = 28800P (주말 포함이므로 10%)
            // 만약 일별 적용이었다면: 80000×0.05 + 104000×0.10 + 104000×0.10 = 24800P
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 3));

            assertThat(result.getTotalPrice()).isEqualTo(288000);
            assertThat(result.getEarnedPoints()).isEqualTo(28800);
        }

        @Test
        @DisplayName("int 캐스팅 소수점 버림 확인")
        void intTruncation() {
            // B 사이트 토요일 1일: 50000 × 1.3 = 65000
            // 65000 × 10% = 6500 (정수로 떨어짐)
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 2, 2), LocalDate.of(2030, 2, 2));

            assertThat(result.getEarnedPoints()).isEqualTo(6500);
        }
    }
}
