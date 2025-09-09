package com.camping.legacy.controller;

import com.camping.legacy.AcceptanceTest;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.SiteAvailabilityResponse;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SiteAcceptanceTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        campsiteRepository.saveAll(
                List.of(
                        new Campsite("A-1", "A-1", 3), // 대형
                        new Campsite("B-1", "B-1", 2)  // 중형
                )
        );
    }

    @CsvSource({"대형, A", "소형, B"})
    @ParameterizedTest
    void 사이트_크기별_예약_가능_사이트_조회_성공(String size, String expectedSiteNumberPrefix) {
        Map<String, Object> queryParams = defaultSearchRequest();
        queryParams.put("size", size);

        // when
        ExtractableResponse<Response> response = sendSearchRequest(queryParams);

        // then
        List<SiteAvailabilityResponse> list = response.jsonPath().getList(".", SiteAvailabilityResponse.class);
        assertThat(list).isNotEmpty();
        assertThat(list).allMatch(site -> site.getSiteNumber().startsWith(expectedSiteNumberPrefix));
    }

    @Test
    void 연박_조건을_만족하는_사이트만_조회() {
        // given
        Map<String, Object> siteB1ReservationRequest = Map.of(
                "customerName", "홍길동",
                "phoneNumber", "010-1234-5678",
                "siteNumber", "B-1",
                "startDate", "2025-09-12",
                "endDate", "2025-09-13"
        );
        ReservationTestHelper.sendReservationCreateRequest(siteB1ReservationRequest);

        // when
        Map<String, Object> queryParams = Map.of(
                "startDate", "2025-09-11",
                "endDate", "2025-09-14"
        );
        ExtractableResponse<Response> response = sendSearchRequest(queryParams);

        // then
        List<SiteAvailabilityResponse> list = response.jsonPath().getList(".", SiteAvailabilityResponse.class);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getSiteNumber()).isEqualTo("A-1");
    }

    private Map<String, Object> defaultSearchRequest() {
        return new HashMap<>(Map.of(
                "startDate", "2025-09-01",
                "endDate", "2025-09-03"
        ));
    }

    private ExtractableResponse<Response> sendSearchRequest(Map<String, Object> queryParams) {
        return RestAssured
                .given()
                .queryParams(queryParams)
                .log().all()
                .when()
                .get("/api/sites/search")
                .then()
                .log().all()
                .extract();
    }
}