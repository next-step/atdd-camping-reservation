package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@DisplayName("Feature: 사이트 조회")
public class SiteLookupTest extends AcceptanceTest {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Test
    @DisplayName("Scenario: 정상 - 전체 사이트 조회")
    void findAllSites() {
        // Given: campsites exist
        campsiteRepository.save(new Campsite("A-1", "Test site A", 4));
        campsiteRepository.save(new Campsite("B-1", "Test site B", 2));

        // When: I GET "/api/sites"
        // Then: response status is 200 and list contains sites
        get("/api/sites")
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2));
    }

    @Test
    @DisplayName("Scenario: 예외 - 사이트가 없으면 빈 목록")
    void findSitesWhenNoneExist() {
        // Given: no campsites exist
        // (Handled by setUp)

        // When: I GET "/api/sites"
        // Then: response status is 200 and list is empty
        get("/api/sites")
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(0));
    }
}
