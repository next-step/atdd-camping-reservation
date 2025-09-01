package com.camping.legacy.controller;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SiteControllerTest {
    
    @LocalServerPort
    int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
    
    @DisplayName("사이트 조회 - 전체 사이트 목록 조회")
    @Test
    void getAllSites_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 사이트 목록을 확인하려는 사용자
         * 무엇을(What): 전체 사이트 목록을
         * 언제(When): 사이트 정보가 필요할 때
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 사용 가능한 모든 사이트를 확인하기 위해
         * 어떻게(How): 사이트 목록 API를 호출해서
         */

        /*
         * given - 사이트들이 등록되어 있다
         * when - 전체 사이트 목록을 조회하면
         * then - 사이트 목록이 반환된다
         */

        // When - 전체 사이트 목록을 조회한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .when().get("/api/sites")
                .then().log().all()
                .extract();
        
        // Then - 사이트 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> sites = response.jsonPath().getList("$");
        assertThat(sites).isNotEmpty();
        
        // 사이트 구조 검증
        Map<String, Object> firstSite = sites.get(0);
        assertThat(firstSite).containsKeys("id", "siteNumber", "size", "description");
    }
    
    @DisplayName("사이트 조회 - 사이트 상세 조회")
    @Test
    void getSiteDetail_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 특정 사이트 정보를 확인하려는 사용자
         * 무엇을(What): 특정 사이트의 상세 정보를
         * 언제(When): 사이트 정보가 필요할 때
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 특정 사이트의 크기, 설명 등을 확인하기 위해
         * 어떻게(How): 사이트 ID로 상세 조회 API를 호출해서
         */

        /*
         * given - 조회할 사이트 ID가 있다
         * when - 특정 사이트를 조회하면
         * then - 사이트 상세 정보가 반환된다
         */

        // Given - 조회할 사이트 ID
        Long siteId = 1L;
        
        // When - 특정 사이트를 조회한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .when().get("/api/sites/" + siteId)
                .then().log().all()
                .extract();
        
        // Then - 사이트 상세 정보가 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getLong("id")).isEqualTo(siteId);
        assertThat(response.jsonPath().getString("siteNumber")).isNotNull();
        assertThat(response.jsonPath().getString("size")).isNotNull();
    }
    
    @DisplayName("사이트 가용성 조회 - 단일 날짜 가용성 확인")
    @Test
    void checkSiteAvailability_Available() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 사용자
         * 무엇을(What): 특정 사이트의 특정 날짜 가용성을
         * 언제(When): 예약 전에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 해당 날짜에 사이트가 예약 가능한지 확인하기 위해
         * 어떻게(How): 사이트 번호와 날짜로 가용성 API를 호출해서
         */

        /*
         * given - 조회할 사이트 번호와 날짜가 있다
         * when - 특정 사이트의 특정 날짜 가용성을 확인하면
         * then - 가용성 정보가 반환된다
         */

        // Given - 조회할 사이트 번호와 날짜
        String siteNumber = "A001";
        LocalDate checkDate = LocalDate.now().plusDays(5);
        
        // When - 특정 사이트의 특정 날짜 가용성을 확인한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("date", checkDate.toString())
                .when().get("/api/sites/" + siteNumber + "/availability")
                .then().log().all()
                .extract();
        
        // Then - 가용성 정보가 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(siteNumber);
        assertThat(response.jsonPath().getString("date")).isEqualTo(checkDate.toString());
        assertThat(response.jsonPath().getBoolean("available")).isNotNull();
    }
    
    @DisplayName("사이트 가용성 조회 - 날짜별 가용 사이트 목록")
    @Test
    void getAvailableSites_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 사용자
         * 무엇을(What): 특정 날짜에 가용한 모든 사이트 목록을
         * 언제(When): 특정 날짜에 대해
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 해당 날짜에 예약 가능한 모든 사이트를 보기 위해
         * 어떻게(How): 날짜로 가용 사이트 API를 호출해서
         */

        /*
         * given - 조회할 날짜가 있다
         * when - 해당 날짜에 가능한 모든 사이트를 조회하면
         * then - 가용 사이트 목록이 반환된다
         */

        // Given - 조회할 날짜
        LocalDate searchDate = LocalDate.now().plusDays(7);
        
        // When - 해당 날짜에 가능한 모든 사이트를 조회한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("date", searchDate.toString())
                .when().get("/api/sites/available")
                .then().log().all()
                .extract();
        
        // Then - 가용 사이트 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        assertThat(availableSites).isNotNull();
    }
    
    @DisplayName("사이트 검색 - 기간별 가용 사이트 검색")
    @Test
    void searchAvailableSites_DateRange() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 연박 예약을 하려는 사용자
         * 무엇을(What): 전체 기간 동안 가용한 사이트 목록을
         * 언제(When): 특정 기간 동안
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 연박 예약을 위해 전체 기간이 비어있는 사이트를 찾기 위해
         * 어떻게(How): 시작일과 종료일로 사이트 검색 API를 호출해서
         */

        /*
         * given - 대형 사이트와 소형 사이트가 있다
         * when - 해당 기간 동안 가용한 사이트를 검색하면
         * then - 전체 기간 가용한 사이트 목록이 반환된다
         */

        // Given - 대형 사이트 "A001", "A002"와 소형 사이트 "B001", "B002"가 있다
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(2);
        
        // When - 해당 기간 동안 가용한 사이트를 검색한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
        
        // Then - 전체 기간 가용한 사이트 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        assertThat(availableSites).isNotNull();
    }
    
    @DisplayName("사이트 검색 - 사이트 크기별 필터링")
    @Test
    void searchAvailableSites_FilterBySize() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 특정 크기의 사이트만 원하는 사용자
         * 무엇을(What): 대형 사이트만을
         * 언제(When): 특정 기간 동안
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 특정 크기의 사이트만 필요하기 때문에
         * 어떻게(How): 크기 필터를 사용하여 사이트 검색 API를 호출해서
         */

        /*
         * given - 대형 사이트와 소형 사이트가 있다
         * when - "대형" 사이트만 검색하면
         * then - 대형 사이트만 검색 결과에 나타난다
         */

        // Given - 대형 사이트만 검색하는 조건
        LocalDate startDate = LocalDate.now().plusDays(12);
        LocalDate endDate = startDate.plusDays(2);
        String sizeFilter = "대형";
        
        // When - 대형 사이트만 검색한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", sizeFilter)
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
        
        // Then - 대형 사이트만 검색 결과에 나타난다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        
        // 결과가 있다면 모두 대형 사이트여야 함
        availableSites.forEach(site -> {
            String siteNumber = (String) site.get("siteNumber");
            if (siteNumber != null) {
                // A로 시작하는 사이트는 대형
                assertThat(siteNumber).startsWith("A");
            }
        });
    }
    
    @DisplayName("사이트 검색 - 이미 예약된 사이트는 검색에서 제외")
    @Test
    void searchAvailableSites_ExcludeReservedSites() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 사용자
         * 무엇을(What): 가용한 사이트 목록을
         * 언제(When): 이미 예약된 사이트가 있는 기간에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 이미 예약된 사이트는 예약할 수 없기 때문에
         * 어떻게(How): 예약된 사이트가 있는 기간에 사이트 검색을 해서
         */

        /*
         * given - 특정 사이트가 특정 기간에 예약되어 있다
         * when - 해당 기간의 가용 사이트를 검색하면
         * then - 예약된 사이트는 검색 결과에 나타나지 않는다
         */

        // Given - 사이트 "A001"이 특정 기간에 예약되어 있다
        String reservedSiteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(15);
        LocalDate endDate = startDate.plusDays(2);
        
        // 먼저 예약을 생성
        RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", "예약자",
                        "phoneNumber", "010-0000-0000",
                        "campsiteId", reservedSiteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value());
        
        // When - 해당 기간의 가용 사이트를 검색한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
        
        // Then - 예약된 사이트는 검색 결과에 나타나지 않는다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        
        // 예약된 사이트가 결과에 없는지 확인
        boolean containsReservedSite = availableSites.stream()
                .anyMatch(site -> reservedSiteId.equals(String.valueOf(site.get("id"))));
        assertThat(containsReservedSite).isFalse();
    }
    
    @DisplayName("사이트 검색 - 취소된 예약 사이트 재예약 가능")
    @Test
    void searchAvailableSites_CancelledReservationAvailable() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 취소된 예약 사이트를 다시 예약하려는 사용자
         * 무엇을(What): 취소된 사이트의 가용성을
         * 언제(When): 예약이 취소된 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 취소된 예약은 중복 체크에서 제외되어야 하기 때문에
         * 어떻게(How): 예약을 취소한 후 해당 사이트를 검색해서
         */

        /*
         * given - 사이트가 예약되었다가 취소된 상태이다
         * when - 해당 날짜에 사이트를 새로 예약 가능한지 확인하면
         * then - 취소된 사이트가 검색 결과에 나타난다
         */

        // Given - 사이트가 예약되었다가 취소된 상태이다
        String siteId = "2";
        LocalDate startDate = LocalDate.now().plusDays(18);
        LocalDate endDate = startDate.plusDays(1);
        
        // 예약 생성
        ExtractableResponse<Response> createResponse = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", "김철수",
                        "phoneNumber", "010-1234-5678",
                        "campsiteId", siteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract();
        
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        
        // 예약 취소
        RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().statusCode(HttpStatus.OK.value());
        
        // When - 해당 날짜에 사이트를 새로 예약 가능한지 확인한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
        
        // Then - 취소된 사이트가 검색 결과에 나타난다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> availableSites = response.jsonPath().getList("$");
        
        boolean containsCancelledSite = availableSites.stream()
                .anyMatch(site -> siteId.equals(String.valueOf(site.get("id"))));
        assertThat(containsCancelledSite).isTrue();
    }
}