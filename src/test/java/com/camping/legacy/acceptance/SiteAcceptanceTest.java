package com.camping.legacy.acceptance;

import com.camping.legacy.support.TestBase;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("사이트 시스템 인수 테스트")
class SiteAcceptanceTest extends TestBase {

    @DisplayName("예약된 사이트가 있는 기간에 가용 사이트를 검색하면, 예약된 사이트는 검색 결과에 나타나지 않는다")
    @Test
    void scenario_SearchAvailableSitesByPeriod() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 가용한 사이트를 찾려는 사용자
         * 무엇을(What): 특정 기간에 가용한 사이트 목록을
         * 언제(When): 사이트 예약이 있는 기간에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 이미 예약된 사이트는 검색 결과에서 제외되어야 하기 때문에
         * 어떻게(How): 이미 예약된 사이트가 있는 기간에 사이트 검색을 해서
         */

        // Given: 사이트 "1"이 특정 기간 예약되어 있다
        String reservedSiteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(28);
        LocalDate endDate = startDate.plusDays(3);

        createReservation("예약자", "010-0000-0000", reservedSiteId, startDate, endDate);

        // When: 예약된 사이트가 있는 기간에 가용 사이트를 검색하면
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();

        // Then: 예약된 사이트는 검색 결과에 나타나지 않는다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");

        boolean containsReservedSite = availableSites.stream()
                .anyMatch(site -> reservedSiteId.equals(String.valueOf(site.get("id"))));
        assertThat(containsReservedSite).isFalse();
    }

    @DisplayName("대형 사이트 필터로 사이트를 검색하면, A로 시작하는 대형 사이트만 검색 결과에 나타난다")
    @Test
    void scenario_FilterSitesBySize() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 특정 크기의 사이트만 원하는 사용자
         * 무엇을(What): 대형 사이트만을
         * 언제(When): 특정 기간 동안
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 대형 사이트만 필요하기 때문에
         * 어떻게(How): "대형" 크기 필터를 사용하여 사이트 검색을 해서
         */

        // Given: 대형 사이트와 소형 사이트가 있다
        LocalDate searchStart = LocalDate.now().plusDays(29);
        LocalDate searchEnd = searchStart.plusDays(2);
        String sizeFilter = "대형";

        // When: 대형 사이트 필터로 사이트를 검색하면
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("startDate", searchStart.toString())
                .queryParam("endDate", searchEnd.toString())
                .queryParam("size", sizeFilter)
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();

        // Then: A로 시작하는 대형 사이트만 검색 결과에 나타난다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");

        // A로 시작하는 사이트는 대형
        availableSites.forEach(site -> {
            String siteNumber = (String) site.get("siteNumber");
            if (siteNumber != null && !siteNumber.isEmpty()) {
                assertThat(siteNumber).startsWith("A");
            }
        });
    }
}
