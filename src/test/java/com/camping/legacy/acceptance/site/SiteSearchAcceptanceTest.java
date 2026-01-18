package com.camping.legacy.acceptance.site;

import com.camping.legacy.acceptance.ApiAcceptanceTestBase;
import com.camping.legacy.acceptance.fixtures.ReservationRequest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.matcher.AcceptanceAssertions.assertThatResponse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see "file:docs/features/site-search.feature"
 */
@SuppressWarnings("NonAsciiCharacters")
class SiteSearchAcceptanceTest extends ApiAcceptanceTestBase {

    @Test
    void 예약_가능_여부_조회에서_예약_기간에_포함되면_available_false() {
        사이트를_생성한다("A-1");

        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        예약을_생성한다(ReservationRequest.builder()
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
        assertThatResponse(response)
                .status(200)
                .response(it -> {
                    assertThat(it.getBoolean("available")).isFalse();
                    assertThat(it.getString("siteNumber")).isEqualTo("A-1");
                });
    }

    @Test
    void 특정_날짜에_예약_가능한_사이트_목록을_반환한다() {
        사이트를_생성한다("A-1");
        사이트를_생성한다("B-1");

        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        예약을_생성한다(ReservationRequest.builder()
                .customerName("홍길동")
                .startDate(start)
                .endDate(end)
                .siteNumber("A-1")
                .build());

        // When
        ExtractableResponse<Response> response = 특정_날짜에_예약_가능한_사이트_목록을_조회한다(start.toString());

        // Then
        assertThatResponse(response)
                .status(200)
                .response(it -> {
                    assertThat(it.getList("siteNumber")).doesNotContain("A-1");
                    assertThat(it.getList("siteNumber")).contains("B-1");
                });
    }

    @Test
    void 검색_결과에는_기간_내_모든_날짜가_예약_가능한_사이트만_포함된다() {
        사이트를_생성한다("A-1");
        사이트를_생성한다("B-1");

        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);

        예약을_생성한다(ReservationRequest.builder()
                .customerName("홍길동")
                .startDate(start)
                .endDate(end)
                .siteNumber("A-1")
                .build());

        // When
        ExtractableResponse<Response> response = 기간으로_사이트를_검색한다(start.toString(), end.toString());

        // Then
        assertThatResponse(response)
                .status(200)
                .response(it -> assertThat(it.getList("siteNumber")).doesNotContain("A-1"));
    }

    @Test
    void 예외_과거_날짜로_기간_검색을_시도하면_실패한다() {
        LocalDate start = LocalDate.now().minusDays(3);
        LocalDate end = LocalDate.now().minusDays(1);

        ExtractableResponse<Response> response = 기간으로_사이트를_검색한다(start.toString(), end.toString());

        assertThatResponse(response)
                .response(it -> {
                    assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
                    assertThat(response.asString()).contains("과거 날짜는 검색할 수 없습니다.");
                });
    }

    @Test
    void 예외_종료일이_시작일보다_이전이면_기간_검색이_실패한다() {
        LocalDate start = LocalDate.now().plusDays(12);
        LocalDate end = LocalDate.now().plusDays(10);

        ExtractableResponse<Response> response = 기간으로_사이트를_검색한다(start.toString(), end.toString());

        assertThatResponse(response)
                .response(it -> {
                    assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
                    assertThat(response.asString()).contains("종료일이 시작일보다 이전일 수 없습니다.");
                });
    }
}
