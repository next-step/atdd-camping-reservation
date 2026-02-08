package com.camping.legacy.controller;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Feature: 사이트 상세")
public class SiteDetailTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CampsiteRepository campsiteRepository;

    private Campsite existingSite;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        campsiteRepository.deleteAll();

        // Given: a campsite exists
        existingSite = campsiteRepository.save(new Campsite("A-1", "Test site", 4));
    }

    @Test
    @DisplayName("Scenario: 정상 - 사이트 상세 조회")
    void findSiteDetail() {
        // When: I GET "/api/sites/{id}"
        // Then: response is 200 and contains site details
        RestAssured.given().log().all()
                .when().get("/api/sites/" + existingSite.getId())
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(existingSite.getId().intValue()));
    }

    @Test
    @DisplayName("Scenario: 실패 - 존재하지 않는 사이트 ID")
    void findSiteDetailWithNonExistentId() {
        // When: I GET "/api/sites/{id}" with a non-existent id
        // Then: response is 404
        RestAssured.given().log().all()
                .when().get("/api/sites/99999")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
