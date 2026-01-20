package com.camping.legacy.assertion;

import static com.camping.legacy.client.SiteClient.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;

public class SiteAssertion {

    public static void 검색_결과에_해당_사이트가_포함된다(ExtractableResponse<Response> response, String... siteNumbers) {
        List<String> resultNumbers = response.jsonPath().getList("siteNumber");
        assertThat(resultNumbers).contains(siteNumbers);
    }

    public static void 검색_결과에_특정_사이트는_포함되지_않는다(
            ExtractableResponse<Response> response, String siteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).doesNotContain(siteNumber);
    }

    public static void 검색_결과에_특정_사이트_하나만_포함된다(
            ExtractableResponse<Response> response, String siteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).hasSize(1);
        assertThat(siteNumbers).containsExactly(siteNumber);
    }

    public static void 모든_사이트는_이용가능한_상태이다(ExtractableResponse<Response> response) {
        List<Boolean> availability = response.jsonPath().getList("available");
        assertThat(availability).allMatch(available -> available);
    }

    public static void 특정_기간에_사이트가_노출됨을_확인한다(int startOffset, int endOffset, String siteType, String expectedSiteNumber) {
        var response = 사이트를_검색한다(startOffset, endOffset, siteType);

        List<String> resultNumbers = response.jsonPath().getList("siteNumber");
        assertThat(resultNumbers).contains(expectedSiteNumber);
    }

    public static void 검색_결과에_최대인원_정보가_있다(ExtractableResponse<Response> response) {
        List<Integer> maxPeopleList = response.jsonPath().getList("maxPeople");
        assertThat(maxPeopleList).allMatch(people -> people > 0);
    }

    public static void 검색_결과에_전기가능여부_정보가_있다(ExtractableResponse<Response> response) {
        List<Boolean> electricityList = response.jsonPath().getList("hasElectricity");
        assertThat(electricityList).allMatch(Objects::nonNull);
    }

    public static void 검색_요청이_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
