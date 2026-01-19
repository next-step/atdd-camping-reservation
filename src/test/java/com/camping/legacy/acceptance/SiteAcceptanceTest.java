package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.fixture.SiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캠핑 사이트 인수 테스트")
class SiteAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        CampsiteFixture.createDefaultSites(campsiteRepository);
    }

    @Test
    @DisplayName("전체 캠핑 사이트 목록을 조회한다")
    void shouldReturnAllSites() {
        // when
        ExtractableResponse<Response> response = SiteFixture.getAllSites();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("")).isNotEmpty();
        assertThat(response.jsonPath().getString("[0].siteNumber")).isNotNull();
    }

    @Test
    @DisplayName("특정 캠핑 사이트 상세 정보를 조회한다")
    void shouldReturnSiteDetail() {
        // when
        ExtractableResponse<Response> response = SiteFixture.getSiteById(1L);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getLong("id")).isEqualTo(1L);
        assertThat(response.jsonPath().getString("siteNumber")).isNotNull();
    }
}
