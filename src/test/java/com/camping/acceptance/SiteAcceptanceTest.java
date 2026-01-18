package com.camping.acceptance;

import com.camping.legacy.domain.Campsite;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.List;
import static com.camping.acceptance.ReservationSteps.예약_요청_생성;
import static com.camping.acceptance.ReservationSteps.예약을_생성한다;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캠핑 사이트 인수 테스트")
public class SiteAcceptanceTest extends AcceptanceTest {

    public static final String API_SITES_SEARCH = "/api/sites/search";
    public static final String API_SITES = "/api/sites";

    private Campsite siteA01, siteA02, siteB01;

    @BeforeEach
    void setUp() {
        super.setUp(); // 명시적으로 상위 클래스의 setUp 호출
        siteA01 = campsiteRepository.save(Campsite.builder().siteNumber("A-01").maxPeople(4).description("A-01").build());
        siteA02 = campsiteRepository.save(Campsite.builder().siteNumber("A-02").maxPeople(4).description("A-02").build());
        siteB01 = campsiteRepository.save(Campsite.builder().siteNumber("B-01").maxPeople(2).description("B-01").build());
    }

    @Test
    @DisplayName("날짜를_지정하여_예약_가능한_모든_사이트를_조회한다")
    void searchAvailableSitesByDate() {
        // given
        var searchDate = LocalDate.of(2025, 11, 15);
        var request = 예약_요청_생성(siteA01.getSiteNumber(), searchDate, searchDate.plusDays(2), "김예약", "010-1111-1111");
        예약을_생성한다(request);

        // when
        var response = 예약_가능_사이트를_검색한다(searchDate, searchDate.plusDays(1), null);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).contains(siteA02.getSiteNumber(), siteB01.getSiteNumber());
        assertThat(availableSites).doesNotContain(siteA01.getSiteNumber());
    }

    @Test
    @DisplayName("예약_가능한_대형_사이트만_필터링하여_조회한다")
    void searchAvailableSitesByDateAndSize() {
        // given
        var searchDate = LocalDate.of(2025, 12, 25);

        // when
        var response = 예약_가능_사이트를_검색한다(searchDate, searchDate.plusDays(1), "A");

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> availableSites = response.jsonPath().getList("siteNumber", String.class);
        assertThat(availableSites).contains(siteA01.getSiteNumber(), siteA02.getSiteNumber());
        assertThat(availableSites).doesNotContain(siteB01.getSiteNumber());
    }

    @Test
    @DisplayName("특정_캠핑_사이트의_상세_정보를_조회한다")
    void getSiteDetails() {
        // given
        var site = campsiteRepository.save(Campsite.builder().siteNumber("C-07").maxPeople(6).description("강가 전망 사이트").build());

        // when
        var response = 사이트_상세_정보를_조회한다(site.getId());

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("C-07");
        assertThat(response.jsonPath().getInt("maxPeople")).isEqualTo(6);
        assertThat(response.jsonPath().getString("description")).contains("강가 전망");
    }

    // --- Helper Methods ---

    private ExtractableResponse<Response> 예약_가능_사이트를_검색한다(LocalDate startDate, LocalDate endDate, String size) {
        return RestAssured
                .given().log().all()
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("size", size)
                .when()
                .get(API_SITES_SEARCH)
                .then().log().all()
                .extract();
    }
    
    private ExtractableResponse<Response> 사이트_상세_정보를_조회한다(Long siteId) {
        return RestAssured
                .given().log().all()
                .when()
                .get(API_SITES + "/" + siteId)
                .then().log().all()
                .extract();
    }
}
