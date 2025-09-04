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

    // Given - 고객 정보 설정
    private CustomerInfo 예약자() {
        return new CustomerInfo("예약자", "010-0000-0000");
    }

    // Given - 날짜 설정
    private LocalDate 미래날짜(int days) {
        return LocalDate.now().plusDays(days);
    }

    // When - 가용 사이트 검색
    private ExtractableResponse<Response> 가용_사이트_검색(LocalDate startDate, LocalDate endDate, String size) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", size)
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 가용_사이트_검색(LocalDate startDate, LocalDate endDate) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    // Then - 사이트 검색 결과 검증
    private void 사이트_검색_성공_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    private void 예약된_사이트_제외_검증(ExtractableResponse<Response> response, String reservedSiteId) {
        사이트_검색_성공_검증(response);
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        boolean containsReservedSite = availableSites.stream()
                .anyMatch(site -> reservedSiteId.equals(String.valueOf(site.get("id"))));
        assertThat(containsReservedSite).isFalse();
    }

    private void 대형_사이트만_검증(ExtractableResponse<Response> response) {
        사이트_검색_성공_검증(response);
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        availableSites.forEach(site -> {
            String siteNumber = (String) site.get("siteNumber");
            if (siteNumber != null && !siteNumber.isEmpty()) {
                assertThat(siteNumber).startsWith("A");
            }
        });
    }

    // Helper class for customer info
    private static class CustomerInfo {
        final String name;
        final String phoneNumber;

        CustomerInfo(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }

    @DisplayName("예약된 사이트가 있는 기간에 가용 사이트를 검색하면, 예약된 사이트는 검색 결과에 나타나지 않는다")
    @Test
    void scenario_SearchAvailableSitesByPeriod() {
        // Given: 사이트 "A001"이 특정 기간 예약되어 있다
        String reservedSiteId = "A001";
        LocalDate startDate = 미래날짜(28);
        LocalDate endDate = startDate.plusDays(3);
        CustomerInfo customer = 예약자();
        
        createReservation(customer.name, customer.phoneNumber, reservedSiteId, startDate, endDate);

        // When: 예약된 사이트가 있는 기간에 가용 사이트를 검색하면
        ExtractableResponse<Response> response = 가용_사이트_검색(startDate, endDate);

        // Then: 예약된 사이트는 검색 결과에 나타나지 않는다
        예약된_사이트_제외_검증(response, reservedSiteId);
    }

    @DisplayName("대형 사이트 필터로 사이트를 검색하면, A로 시작하는 대형 사이트만 검색 결과에 나타난다")
    @Test
    void scenario_FilterSitesBySize() {
        // Given: 대형 사이트와 소형 사이트가 있다
        LocalDate searchStart = 미래날짜(29);
        LocalDate searchEnd = searchStart.plusDays(2);
        String sizeFilter = "대형";

        // When: 대형 사이트 필터로 사이트를 검색하면
        ExtractableResponse<Response> response = 가용_사이트_검색(searchStart, searchEnd, sizeFilter);

        // Then: A로 시작하는 대형 사이트만 검색 결과에 나타난다
        대형_사이트만_검증(response);
    }
}
