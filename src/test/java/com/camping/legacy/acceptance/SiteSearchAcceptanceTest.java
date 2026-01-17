package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.acceptance.ReservationSteps.createReservation;
import static com.camping.legacy.acceptance.SiteSteps.searchAvailableSites;
import static com.camping.legacy.acceptance.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteSearchAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("연박 중간 날짜와 겹치는 기간 검색 시 해당 사이트 제외")
    void 연박_중간_날짜_겹침_검색() {
        // Given - 10~15일 예약
        createReservation(DEFAULT_CUSTOMER, SITE_A1, 10, 15);

        // When - 8~12일 가용 사이트 검색
        var response = searchAvailableSites(8, 12);

        // Then - A-1 사이트 제외 검증
        assertThat(response.jsonPath().getList("siteNumber")).doesNotContain(SITE_A1);
    }
}
