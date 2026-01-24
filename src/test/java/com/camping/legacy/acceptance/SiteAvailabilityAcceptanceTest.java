package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.fixture.ReservationFixture;
import com.camping.legacy.fixture.SiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("사이트 가용성 확인 인수 테스트")
class SiteAvailabilityAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        CampsiteFixture.createAllSites(campsiteRepository);
    }

    @Nested
    @DisplayName("단일 날짜 조회")
    class SingleDateQuery {

        @Test
        @DisplayName("예약된 사이트는 가용 목록에서 제외된다")
        void shouldExcludeReservedSiteFromAvailableList() {
            // given
            var targetDate = LocalDate.now().plusDays(7);
            ReservationFixture.createReservation(
                    ReservationFixture.createRequest("김철수", "A-1", targetDate, targetDate.plusDays(2)));

            // when
            var response = SiteFixture.getAvailableSites(targetDate);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            var siteNumbers = response.jsonPath().getList("siteNumber", String.class);
            assertThat(siteNumbers).contains("A-2");
            assertThat(siteNumbers).doesNotContain("A-1");
        }
    }

    @Nested
    @DisplayName("기간 검색")
    class PeriodSearch {

        @Test
        @DisplayName("기간 내 예약이 있는 사이트는 검색 결과에서 제외된다")
        void shouldExcludeReservedSiteFromPeriodSearch() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(10);
            ReservationFixture.createReservation(
                    ReservationFixture.createRequest("김철수", "A-1", startDate.plusDays(1), startDate.plusDays(2)));

            // when
            var response = SiteFixture.searchAvailableSites(startDate, endDate);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            var siteNumbers = response.jsonPath().getList("siteNumber", String.class);
            assertThat(siteNumbers).doesNotContain("A-1");
        }
    }

    @Nested
    @DisplayName("취소 반영")
    class CancellationReflection {

        @Test
        @DisplayName("취소된 예약의 사이트는 가용 목록에 표시된다")
        void shouldShowCancelledSiteAsAvailable() {
            // given
            var targetDate = LocalDate.now().plusDays(7);
            var reservation = ReservationFixture.createReservationAndVerify(
                    ReservationFixture.createRequest("김철수", "A-1", targetDate, targetDate.plusDays(2)));
            ReservationFixture.cancelReservation(reservation.getId(), reservation.getConfirmationCode());

            // when
            var response = SiteFixture.getAvailableSites(targetDate);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            var siteNumbers = response.jsonPath().getList("siteNumber", String.class);
            assertThat(siteNumbers).contains("A-1");
        }
    }

    @Nested
    @DisplayName("필터링")
    class Filtering {

        @Test
        @DisplayName("사이즈 필터로 대형 사이트만 검색한다")
        void shouldFilterBySizeLarge() {
            // given
            var startDate = LocalDate.now().plusDays(7);
            var endDate = LocalDate.now().plusDays(10);

            // when
            var response = SiteFixture.searchAvailableSitesWithSize(startDate, endDate, "대형");

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            var siteNumbers = response.jsonPath().getList("siteNumber", String.class);
            assertThat(siteNumbers).allMatch(sn -> sn.startsWith("A"));
            assertThat(siteNumbers).noneMatch(sn -> sn.startsWith("B"));
        }
    }

    @Nested
    @DisplayName("특정 사이트 가용성 확인")
    class SpecificSiteAvailability {

        @Test
        @DisplayName("예약 가능한 사이트 조회 시 available이 true이다")
        void shouldReturnAvailableTrueWhenNotReserved() {
            // given
            var targetDate = LocalDate.now().plusDays(7);

            // when
            var response = SiteFixture.checkSiteAvailability("A-1", targetDate);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-1");
            assertThat(response.jsonPath().getBoolean("available")).isTrue();
        }

        @Test
        @DisplayName("예약된 사이트 조회 시 available이 false이다")
        void shouldReturnAvailableFalseWhenReserved() {
            // given
            var targetDate = LocalDate.now().plusDays(7);
            ReservationFixture.createReservation(
                    ReservationFixture.createRequest("김철수", "A-1", targetDate, targetDate.plusDays(2)));

            // when
            var response = SiteFixture.checkSiteAvailability("A-1", targetDate);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-1");
            assertThat(response.jsonPath().getBoolean("available")).isFalse();
        }
    }

    @Nested
    @DisplayName("예외")
    class ErrorCases {

        @Test
        @DisplayName("과거 날짜로 가용성 조회 시 에러가 발생한다")
        void shouldFailWhenCheckingPastDate() {
            // given
            var pastDate = LocalDate.now().minusDays(3);

            // when
            var response = SiteFixture.checkSiteAvailability("A-1", pastDate);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
