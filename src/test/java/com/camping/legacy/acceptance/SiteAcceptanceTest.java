package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.camping.legacy.client.SiteClient;

import static com.camping.legacy.steps.SiteSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캠핑 사이트 인수 테스트")
class SiteAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void 사전_데이터_준비() {
        CampsiteFixture.기본_사이트_생성(campsiteRepository);
    }

    @Test
    @DisplayName("전체 캠핑 사이트 목록을 조회한다")
    void 전체_캠핑_사이트_목록을_조회한다() {
        // when
        var 응답 = SiteClient.전체_사이트_조회_API();

        // then
        응답_성공_확인(응답);
        assertThat(응답.jsonPath().getList("")).isNotEmpty();
        assertThat(응답.jsonPath().getString("[0].siteNumber")).isNotNull();
    }

    @Test
    @DisplayName("특정 캠핑 사이트 상세 정보를 조회한다")
    void 특정_캠핑_사이트_상세_정보를_조회한다() {
        // when
        var 응답 = SiteClient.사이트_상세_조회_API(1L);

        // then
        응답_성공_확인(응답);
        assertThat(응답.jsonPath().getLong("id")).isEqualTo(1L);
        assertThat(응답.jsonPath().getString("siteNumber")).isNotNull();
    }
}
