package com.camping.legacy.acceptance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.acceptance.ReservationSteps.createReservation;
import static com.camping.legacy.acceptance.SiteSteps.searchAvailableSites;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteSearchAcceptanceTest extends AcceptanceTest {

    /**
     * 레거시 버그 BUG-002: 연박 중간 날짜 미확인
     * - 현재 상태: 10~15일 예약 있어도 8~12일 검색 A-1 포함 (버그)
     * - 기대 동작: A-1 사이트 제외
     */
    @Disabled("버그 수정 대상 - 연박 중간 날짜 미확인")
    @Test
    @DisplayName("연박 중간 날짜와 겹치는 기간 검색 시 해당 사이트 제외")
    void 연박_중간_날짜_겹침_검색() {
        // Given - 10~15일 예약
        createReservation("홍길동", "A-1", 10, 15);

        // When - 8~12일 가용 사이트 검색
        var response = searchAvailableSites(8, 12);

        // Then - A-1 사이트 제외 검증
        assertThat(response.jsonPath().getList("siteNumber")).doesNotContain("A-1");
    }
}
