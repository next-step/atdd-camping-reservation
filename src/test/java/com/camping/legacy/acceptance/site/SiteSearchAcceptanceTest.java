package com.camping.legacy.acceptance.site;

import com.camping.legacy.acceptance.ApiAcceptanceTestBase;
import com.camping.legacy.acceptance.fixtures.ReservationRequestFixture;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see "file:docs/features/site-search.feature"
 */
@SuppressWarnings("NonAsciiCharacters")
class SiteSearchAcceptanceTest extends ApiAcceptanceTestBase {

    @Test
    void 예약_가능_여부_조회에서_예약_기간에_포함되면_available_false() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        예약을_생성한다(ReservationRequestFixture.builder()
                .customerName("홍길동")
                .startDate(start)
                .endDate(end)
                .siteNumber("A-1")
                .build());

        // When
        ExtractableResponse<Response> response = 사이트_예약_가능_여부를_조회한다(
                "A-1",
                start.plusDays(1).toString()
        );

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-1");
    }

    @Test
    void 특정_날짜에_예약_가능한_사이트_목록을_반환한다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        예약을_생성한다(ReservationRequestFixture.builder()
                .customerName("홍길동")
                .startDate(start)
                .endDate(end)
                .siteNumber("A-1")
                .build());

        // When
        ExtractableResponse<Response> response = 특정_날짜에_예약_가능한_사이트_목록을_조회한다(start.toString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("siteNumber")).doesNotContain("A-1");
        assertThat(response.jsonPath().getList("siteNumber")).contains("B-1");
    }

    @Test
    void 검색_결과에는_기간_내_모든_날짜가_예약_가능한_사이트만_포함된다() {
        // Given
        // 검색할 기간(3일)
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);

        // 겹치는 예약 3건을 만들어서, 검색 결과에서 해당 사이트들이 제외되는지 확인
        // 1) A-1 : 기간 전체를 덮는 예약(완전 차단)
        예약을_생성한다(ReservationRequestFixture.builder()
                .customerName("홍길동")
                .startDate(start)
                .endDate(end)
                .siteNumber("A-1")
                .build());

        // 2) A-2 : 기간 중 하루만 겹치는 예약(부분 차단)
        예약을_생성한다(ReservationRequestFixture.builder()
                .customerName("임꺽정")
                .startDate(start.plusDays(1))
                .endDate(start.plusDays(1))
                .siteNumber("A-2")
                .build());

        // 3) B-1 : 기간 직전에 끝나는 예약(겹치지 않음 -> 검색 결과에 포함되어야 함)
        예약을_생성한다(ReservationRequestFixture.builder()
                .customerName("김철수")
                .startDate(start.minusDays(2))
                .endDate(start.minusDays(1))
                .siteNumber("B-1")
                .build());

        // When
        ExtractableResponse<Response> response = 기간으로_사이트를_검색한다(start.toString(), end.toString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        var siteNumbers = response.jsonPath().getList("siteNumber");

        // 기간과 겹치는 예약이 있는 사이트는 제외
        assertThat(siteNumbers).doesNotContain("A-1");
        assertThat(siteNumbers).doesNotContain("A-2");

        // 기간 내 모든 날짜가 비어있는 사이트는 포함
        assertThat(siteNumbers).contains("B-1");
    }

    // FIXME 에러 문구 처리되도록 수정 필요
    @Disabled
    @Test
    void 예외_과거_날짜로_기간_검색을_시도하면_실패한다() {
        // Given: 오늘 기준 과거 날짜로 요청
        LocalDate start = LocalDate.now().minusDays(3);
        LocalDate end = LocalDate.now().minusDays(1);

        // When
        ExtractableResponse<Response> response = 기간으로_사이트를_검색한다(start.toString(), end.toString());

        // Then
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.asString()).contains("과거 날짜는 검색할 수 없습니다.");
    }

    // FIXME 에러 문구 처리되도록 수정 필요
    @Disabled
    @Test
    void 예외_종료일이_시작일보다_이전이면_기간_검색이_실패한다() {
        LocalDate start = LocalDate.now().plusDays(12);
        LocalDate end = LocalDate.now().plusDays(10);

        ExtractableResponse<Response> response = 기간으로_사이트를_검색한다(start.toString(), end.toString());

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.asString()).contains("종료일이 시작일보다 이전일 수 없습니다.");
    }
}
