package com.camping.legacy.site;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteSteps {

    public static ExtractableResponse<Response> 전체_사이트_목록_조회_요청() {
        return given().log().all()
                .when().get("/api/sites")
                .then().log().all().extract();
    }

    public static void A구역_대형_사이트들이_반환된다(ExtractableResponse<Response> response) {
        List<String> aSites = response.jsonPath().getList("findAll { it.siteNumber.startsWith('A') }.siteNumber");
        List<String> aSiteSizes = response.jsonPath().getList("findAll { it.siteNumber.startsWith('A') }.size");

        assertThat(aSites.size()).isGreaterThan(0);
        assertThat(aSiteSizes).allMatch(size -> "대형".equals(size));
    }

    public static void B구역_소형_사이트들이_반환된다(ExtractableResponse<Response> response) {
        List<String> bSites = response.jsonPath().getList("findAll { it.siteNumber.startsWith('B') }.siteNumber");
        List<String> bSiteSizes = response.jsonPath().getList("findAll { it.siteNumber.startsWith('B') }.size");

        assertThat(bSites.size()).isGreaterThan(0);
        assertThat(bSiteSizes).allMatch(size -> "소형".equals(size));
    }

    public static void 각_사이트의_최대_수용_인원이_포함된다(ExtractableResponse<Response> response) {
        List<Integer> maxPeopleList = response.jsonPath().getList("maxPeople", Integer.class);

        assertThat(maxPeopleList).isNotEmpty();
        assertThat(maxPeopleList).allMatch(maxPeople -> maxPeople != null && maxPeople > 0);
    }
}
