package com.camping.acceptance;

import com.camping.legacy.domain.Campsite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static com.camping.acceptance.ReservationSteps.예약_요청_생성;
import static com.camping.acceptance.ReservationSteps.예약을_생성한다;
import static com.camping.acceptance.SiteSteps.사이트_상세_정보를_조회한다;
import static com.camping.acceptance.SiteSteps.예약_가능_사이트를_검색한다;
import static com.camping.acceptance.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캠핑 사이트 인수 테스트")
public class SiteAcceptanceTest extends AcceptanceTest {

    private Campsite siteA01, siteA02, siteB01;

    @BeforeEach
    void setUp() {
        super.setUp();
        siteA01 = campsiteRepository.save(createCampsite(SITE_A1_NUMBER));
        siteA02 = campsiteRepository.save(createCampsite("A-02"));
        siteB01 = campsiteRepository.save(createCampsite("B-01"));
    }

    @Test
    @DisplayName("날짜를_지정하여_예약_가능한_모든_사이트를_조회한다")
    void searchAvailableSitesByDate() {
        // given
        var searchDate = LocalDate.of(2025, 11, 15);
        var request = 예약_요청_생성(siteA01.getSiteNumber(), searchDate, searchDate.plusDays(2), CUSTOMER_KIM, "010-1111-1111");
        예약을_생성한다(request);

        // when
        var response = 예약_가능_사이트를_검색한다(searchDate, searchDate.plusDays(1), null);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).contains(siteA02.getSiteNumber(), siteB01.getSiteNumber());
        assertThat(availableSites).doesNotContain(siteA01.getSiteNumber());
    }

    @Test
    @DisplayName("예약_가능한_대형_사이트만_필터링하여_조회한다")
    void searchAvailableSitesByDateAndSize() {
        // given
        var searchDate = LocalDate.of(2025, 12, 25);

        // when
        var response = 예약_가능_사이트를_검색한다(searchDate, searchDate.plusDays(1), "A");

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).contains(siteA01.getSiteNumber(), siteA02.getSiteNumber());
        assertThat(availableSites).doesNotContain(siteB01.getSiteNumber());
    }

    @Test
    @DisplayName("특정_캠핑_사이트의_상세_정보를_조회한다")
    void getSiteDetails() {
        // given
        var site = campsiteRepository.save(createCampsite("C-07",6, "강가 전망 사이트"));
        campsiteRepository.save(site);


        // when
        var response = 사이트_상세_정보를_조회한다(site.getId());

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("C-07");
        assertThat(response.jsonPath().getInt("maxPeople")).isEqualTo(6);
        assertThat(response.jsonPath().getString("description")).contains("강가 전망");
    }
}
