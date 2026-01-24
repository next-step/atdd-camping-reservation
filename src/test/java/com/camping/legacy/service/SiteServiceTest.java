package com.camping.legacy.service;

import com.camping.legacy.dto.SiteSearchRequest;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("사이트 서비스 단위 테스트")
class SiteServiceTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        campsiteRepository.deleteAll();
        CampsiteFixture.전체_사이트_생성(campsiteRepository);
    }

    @Test
    @DisplayName("사이즈 필터로 대형 사이트만 검색한다")
    void 사이즈_필터로_대형_사이트만_검색한다() {
        // given
        var request = new SiteSearchRequest(
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(10),
                "대형"
        );

        // when
        var result = siteService.searchAvailableSites(request);

        // then
        assertThat(result).allMatch(site -> site.getSiteNumber().startsWith("A"));
        assertThat(result).noneMatch(site -> site.getSiteNumber().startsWith("B"));
    }
}
