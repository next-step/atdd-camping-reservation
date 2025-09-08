package com.camping.legacy.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("연박 가용성/검색 인수 테스트")
class SiteAvailabilityAcceptanceTest extends AcceptanceTestBase {
    @Test
    @DisplayName("기간 전체가 가능한 사이트만 반환한다")
    void scenario_연박_기간_중_막힌날_있으면_제외된다() {
        /*
         * 5W1H
         * Who(누가): 예약 가능 여부를 조회하는 고객
         * What(무엇을): 2박 3일 연박 가능 여부 조회
         * When(언제): 중간 하루가 이미 다른 예약으로 선점된 상태에서
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 연박은 '전체 기간'이 모두 비어 있어야만 가능함을 검증하기 위해
         * How(어떻게): 먼저 특정 하루를 선점 예약한 뒤, 그 하루를 포함하는 구간으로 /api/sites/search 조회 요청을 보낸다
         */
        // Given: 특정 사이트의 중간 하루(conflict)가 이미 예약되어 있다
        String siteNumber = anySiteNumber(); // 예: A-1
        var conflict = LocalDate.now().plusDays(20);
        var created = createReservation("선점자", "010-0000-0000", siteNumber, conflict, conflict);
        assertThat(created.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // When: 고객이 conflict를 포함하는 2박 3일 구간으로 연박 가능 여부를 조회한다
        var start = conflict.minusDays(1);
        var end   = conflict.plusDays(1);
        var res = searchAvailableSites(start, end); // GET /api/sites/search

        // Then: 응답은 200 OK이고, 해당 사이트는 결과에서 제외된다
        assertThat(res.statusCode()).isEqualTo(HttpStatus.OK.value());
        var siteNumbers = res.jsonPath().getList("siteNumber", String.class);
        assertThat(siteNumbers).doesNotContain(siteNumber);
    }

    @Test
    @DisplayName("대형/소형 필터가 정확히 적용된다")
    void scenario_사이즈_필터_적용된다() {
        /*
         * 5W1H
         * Who(누가): 사이트를 검색하는 고객
         * What(무엇을): 원하는 크기(대형)로만 사이트 검색
         * When(언제): 정상 예약 가능 기간에
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 크기 필터(대형/소형)가 정확히 적용되는지 검증하기 위해
         * How(어떻게): /api/sites/search에 startDate, endDate와 함께 size=대형 쿼리 파라미터로 조회 요청을 보낸다
         */
        // Given: 데이터에 대형/소형 사이트가 공존한다 (test-data.sql 시드)
        var start = LocalDate.now().plusDays(8);
        var end   = start.plusDays(1);

        // When: 고객이 대형만 필터링하여 기간 가용성 검색을 수행한다
        var res = searchAvailableSitesBySize(start, end, "대형");

        // Then: 응답은 200 OK이고, 결과의 size는 모두 "대형"이다
        assertThat(res.statusCode()).isEqualTo(HttpStatus.OK.value());
        var sizes = res.jsonPath().getList("size", String.class);
        assertThat(sizes).isNotEmpty();
        assertThat(new java.util.HashSet<>(sizes)).containsOnly("대형");
    }

}
