package com.camping.legacy.service;

import com.camping.legacy.dto.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feature: 예약 가격 계산")
class ReservationPricingTest {

    private PricingCalculator pricingCalculator;

    @BeforeEach
    void setUp() {
        pricingCalculator = new PricingCalculator();
    }

    // =============================================
    // 기본 가격 테스트
    // =============================================

    @Nested
    @DisplayName("기본 가격")
    class BasePriceTest {

        @Test
        @DisplayName("A 사이트(대형) 평일 비수기 1일 - 80,000원")
        void aSiteBasePrice() {
            // 2030-02-04 = 월요일, 비수기
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 4));

            assertThat(result.getTotalPrice()).isEqualTo(80000);
        }

        @Test
        @DisplayName("B 사이트(소형) 평일 비수기 1일 - 50,000원")
        void bSiteBasePrice() {
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 4));

            assertThat(result.getTotalPrice()).isEqualTo(50000);
        }

        @Test
        @DisplayName("기타 사이트 평일 비수기 1일 - 60,000원")
        void cSiteBasePrice() {
            PricingResult result = pricingCalculator.calculate("C-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 4));

            assertThat(result.getTotalPrice()).isEqualTo(60000);
        }

        @Test
        @DisplayName("A 사이트 평일 비수기 3일 연박 - 240,000원")
        void aSiteMultiDay() {
            // 2030-02-04(월) ~ 02-06(수), 비수기 평일 3일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 6));

            assertThat(result.getTotalPrice()).isEqualTo(240000);
        }
    }

    // =============================================
    // 주말 할증 (30%) 테스트
    // =============================================

    @Nested
    @DisplayName("주말 할증 (30%)")
    class WeekendSurchargeTest {

        @Test
        @DisplayName("A 사이트 토요일 비수기 1일 - 104,000원 (80,000 × 1.3)")
        void aSiteWeekendSaturday() {
            // 2030-02-02 = 토요일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 2), LocalDate.of(2030, 2, 2));

            assertThat(result.getTotalPrice()).isEqualTo(104000);
        }

        @Test
        @DisplayName("B 사이트 일요일 비수기 1일 - 65,000원 (50,000 × 1.3)")
        void bSiteWeekendSunday() {
            // 2030-02-03 = 일요일
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 2, 3), LocalDate.of(2030, 2, 3));

            assertThat(result.getTotalPrice()).isEqualTo(65000);
        }

        @Test
        @DisplayName("A 사이트 토~일 비수기 2일 - 208,000원 (104,000 × 2)")
        void aSiteWeekendTwoDays() {
            // 2030-02-02(토) ~ 02-03(일)
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 2), LocalDate.of(2030, 2, 3));

            assertThat(result.getTotalPrice()).isEqualTo(208000);
        }
    }

    // =============================================
    // 성수기 할증 (50%) 테스트
    // =============================================

    @Nested
    @DisplayName("성수기 할증 (50%)")
    class PeakSeasonSurchargeTest {

        @Test
        @DisplayName("A 사이트 7월 평일 1일 - 120,000원 (80,000 × 1.5)")
        void aSitePeakWeekday() {
            // 2030-07-01 = 월요일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 7, 1), LocalDate.of(2030, 7, 1));

            assertThat(result.getTotalPrice()).isEqualTo(120000);
        }

        @Test
        @DisplayName("B 사이트 8월 평일 1일 - 75,000원 (50,000 × 1.5)")
        void bSitePeakWeekday() {
            // 2030-08-05 = 월요일
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 8, 5), LocalDate.of(2030, 8, 5));

            assertThat(result.getTotalPrice()).isEqualTo(75000);
        }
    }

    // =============================================
    // 성수기 + 주말 할증 (70%) 테스트
    // =============================================

    @Nested
    @DisplayName("성수기 + 주말 할증 (70%)")
    class PeakWeekendSurchargeTest {

        @Test
        @DisplayName("A 사이트 7월 토요일 - 136,000원 (80,000 × 1.7)")
        void aSitePeakWeekend() {
            // 2030-07-06 = 토요일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 7, 6), LocalDate.of(2030, 7, 6));

            assertThat(result.getTotalPrice()).isEqualTo(136000);
        }

        @Test
        @DisplayName("B 사이트 8월 일요일 - 85,000원 (50,000 × 1.7)")
        void bSitePeakWeekend() {
            // 2030-08-04 = 일요일
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 8, 4), LocalDate.of(2030, 8, 4));

            assertThat(result.getTotalPrice()).isEqualTo(85000);
        }
    }

    // =============================================
    // 혼합 기간 (일별 개별 할증) 테스트
    // =============================================

    @Nested
    @DisplayName("혼합 기간 - 일별 개별 할증 적용")
    class MixedPeriodTest {

        @Test
        @DisplayName("A 사이트 금~일(평일1 + 주말2) 비수기 - 288,000원")
        void weekdayAndWeekendMix() {
            // 2030-02-01(금)=80000, 02-02(토)=104000, 02-03(일)=104000
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 3));

            assertThat(result.getTotalPrice()).isEqualTo(288000);
        }

        @Test
        @DisplayName("B 사이트 비수기→성수기 전환(6/30~7/1) - 140,000원")
        void nonPeakToPeakTransition() {
            // 2030-06-30(일)=비수기 주말: 50000×1.3=65000
            // 2030-07-01(월)=성수기 평일: 50000×1.5=75000
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 6, 30), LocalDate.of(2030, 7, 1));

            assertThat(result.getTotalPrice()).isEqualTo(140000);
        }

        @Test
        @DisplayName("A 사이트 성수기 금~일(성수기 평일 + 성수기 주말) - 392,000원")
        void peakWeekdayAndPeakWeekendMix() {
            // 2030-07-05(금)=성수기 평일: 80000×1.5=120000
            // 2030-07-06(토)=성수기 주말: 80000×1.7=136000
            // 2030-07-07(일)=성수기 주말: 80000×1.7=136000
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 7, 5), LocalDate.of(2030, 7, 7));

            assertThat(result.getTotalPrice()).isEqualTo(392000);
        }

        @Test
        @DisplayName("성수기 마지막날→비수기 전환(8/31~9/1) - B 사이트 150,000원")
        void peakToNonPeakTransition() {
            // 2030-08-31(토)=성수기 주말: 50000×1.7=85000
            // 2030-09-01(일)=비수기 주말: 50000×1.3=65000
            PricingResult result = pricingCalculator.calculate("B-1",
                    LocalDate.of(2030, 8, 31), LocalDate.of(2030, 9, 1));

            assertThat(result.getTotalPrice()).isEqualTo(150000);
        }
    }

    // =============================================
    // 1일 예약 (startDate == endDate) 테스트
    // =============================================

    @Nested
    @DisplayName("1일 예약 (startDate == endDate)")
    class SingleDayTest {

        @Test
        @DisplayName("당일 예약 - 1일분 가격만 계산")
        void singleDayReservation() {
            // 2030-02-04 = 월요일
            PricingResult result = pricingCalculator.calculate("A-1",
                    LocalDate.of(2030, 2, 4), LocalDate.of(2030, 2, 4));

            assertThat(result.getTotalPrice()).isEqualTo(80000);
        }
    }
}
