package com.camping.legacy.steps;

import com.camping.legacy.client.SiteClient;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SiteSteps {

    public static List<String> 가용_사이트_조회(LocalDate date) {
        return SiteClient.가용_사이트_조회_API(date)
                .jsonPath().getList("siteNumber", String.class);
    }

    public static List<String> 기간별_가용_사이트_검색(LocalDate startDate, LocalDate endDate) {
        return SiteClient.기간별_가용_사이트_검색_API(startDate, endDate)
                .jsonPath().getList("siteNumber", String.class);
    }

    public static List<String> 사이즈별_가용_사이트_검색(LocalDate startDate, LocalDate endDate, String size) {
        return SiteClient.사이즈별_가용_사이트_검색_API(startDate, endDate, size)
                .jsonPath().getList("siteNumber", String.class);
    }

    public static boolean 사이트_가용_여부_확인(String siteNumber, LocalDate date) {
        return SiteClient.사이트_가용성_확인_API(siteNumber, date)
                .jsonPath().getBoolean("available");
    }

    public static ExtractableResponse<Response> 사이트_가용성_조회_요청(String siteNumber, LocalDate date) {
        return SiteClient.사이트_가용성_확인_API(siteNumber, date);
    }

    public static ExtractableResponse<Response> 전체_사이트_조회_요청() {
        return SiteClient.전체_사이트_조회_API();
    }

    public static ExtractableResponse<Response> 사이트_상세_조회_요청(Long siteId) {
        return SiteClient.사이트_상세_조회_API(siteId);
    }

    public static void 가용_목록에_포함됨(List<String> siteNumbers, String... expectedSites) {
        assertThat(siteNumbers).contains(expectedSites);
    }

    public static void 가용_목록에서_제외됨(List<String> siteNumbers, String... excludedSites) {
        assertThat(siteNumbers).doesNotContain(excludedSites);
    }

    public static void 사이트가_예약_가능함(boolean available) {
        assertThat(available).isTrue();
    }

    public static void 사이트가_예약_불가능함(boolean available) {
        assertThat(available).isFalse();
    }

    public static void 대형_사이트만_포함됨(List<String> siteNumbers) {
        assertThat(siteNumbers).allMatch(siteNumber -> siteNumber.startsWith("A"));
        assertThat(siteNumbers).noneMatch(siteNumber -> siteNumber.startsWith("B"));
    }

    public static void 응답_성공_확인(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 응답_실패_확인(ExtractableResponse<Response> response, HttpStatus status) {
        assertThat(response.statusCode()).isEqualTo(status.value());
    }
}
