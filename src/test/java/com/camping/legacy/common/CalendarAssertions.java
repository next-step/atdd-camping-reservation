package com.camping.legacy.common;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캘린더 관련 Custom Assertions
 */
public class CalendarAssertions {

    /**
     * 특정 날짜가 예약 가능한지 검증
     */
    public static void 날짜가_예약가능_검증(ExtractableResponse<Response> response, String... dates) {
        for (String date : dates) {
            Boolean available = response.jsonPath()
                    .getBoolean("days.find { it.date == '" + date + "' }.available");
            assertThat(available)
                    .as("날짜 %s는 예약 가능해야 합니다", date)
                    .isTrue();
        }
    }

    /**
     * 특정 날짜가 예약됨 상태인지 검증
     */
    public static void 날짜가_예약됨_검증(ExtractableResponse<Response> response, String... dates) {
        for (String date : dates) {
            Boolean available = response.jsonPath()
                    .getBoolean("days.find { it.date == '" + date + "' }.available");
            assertThat(available)
                    .as("날짜 %s는 예약됨 상태여야 합니다", date)
                    .isFalse();
        }
    }

    /**
     * 캘린더 응답의 기본 정보 검증
     */
    public static void 캘린더_기본정보_검증(ExtractableResponse<Response> response,
                                       int expectedYear, int expectedMonth, Long expectedSiteId) {
        assertThat(response.jsonPath().getInt("year")).isEqualTo(expectedYear);
        assertThat(response.jsonPath().getInt("month")).isEqualTo(expectedMonth);
        assertThat(response.jsonPath().getLong("siteId")).isEqualTo(expectedSiteId);
    }

    /**
     * 캘린더에 해당 월의 모든 날짜가 포함되어 있는지 검증
     */
    public static void 월별_날짜수_검증(ExtractableResponse<Response> response, int expectedDays) {
        List<Object> days = response.jsonPath().getList("days");
        assertThat(days)
                .as("해당 월은 %d일이어야 합니다", expectedDays)
                .hasSize(expectedDays);
    }
}