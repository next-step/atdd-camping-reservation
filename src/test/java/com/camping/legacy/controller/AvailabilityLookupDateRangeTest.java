package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@DisplayName("Feature: 가용성 조회(기간)")
public class AvailabilityLookupDateRangeTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("Scenario: 정상 - 기간 가용 사이트 + size 필터")
    void searchAvailableSitesWithSizeFilter() {
        // Given: campsites exist
        campsiteRepository.save(new Campsite("A-1", "대형 사이트", 8));
        campsiteRepository.save(new Campsite("B-1", "소형 사이트", 4));

        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // When: I GET "/api/sites/search?..."
        // Then: response is 200 and list only contains large sites
        Map<String, Object> queryParams = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "size", "대형"
        );
        get("/api/sites/search", queryParams)
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].description", containsString("대형"));
    }

    @Test
    @DisplayName("Scenario: 실패 - 종료일이 시작일보다 이전")
    void searchAvailableSitesWithEndDateBeforeStartDate() {
        LocalDate startDate = LocalDate.now().plusDays(12);
        LocalDate endDate = LocalDate.now().plusDays(10);

        // When: I GET "/api/sites/search?..." with invalid date range
        // Then: an error response is returned
        Map<String, Object> queryParams = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );
        get("/api/sites/search", queryParams)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("Scenario: 실패 - 30일 이후 기간 검색 불가")
    void searchAvailableSitesBeyond30Days() {
        // Given: campsites exist
        campsiteRepository.save(new Campsite("A-1", "대형 사이트", 8));

        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = LocalDate.now().plusDays(33);

        // When: I GET "/api/sites/search?..." with start date beyond 30 days
        // Then: an error response is returned
        Map<String, Object> queryParams = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );
        get("/api/sites/search", queryParams)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
