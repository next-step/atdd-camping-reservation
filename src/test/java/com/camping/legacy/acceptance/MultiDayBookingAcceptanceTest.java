package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_요청;
import static com.camping.legacy.acceptance.steps.SiteSteps.가용_사이트_검색;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 연박 예약 검증 인수 테스트
 *
 * 인수 조건 P0-2:
 * "연박 예약 시 중간 날짜에 기존 예약이 있으면 예약 불가 처리해야 한다"
 */
@DisplayName("연박 예약 검증 인수 테스트")
class MultiDayBookingAcceptanceTest extends AcceptanceTest {

    private static final LocalDate BASE_DATE = LocalDate.now().plusDays(50);

    @Test
    @DisplayName("중간 날짜에 예약이 있으면 연박 예약이 실패한다")
    void 중간_날짜에_예약이_있으면_연박_예약이_실패한다() {
        // Given - B-5에 1/25~26 예약 존재
        LocalDate existingStart = BASE_DATE.plusDays(1);  // 25일
        LocalDate existingEnd = BASE_DATE.plusDays(2);    // 26일
        String siteNumber = "B-5";

        ExtractableResponse<Response> existingReservation = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", existingStart, existingEnd);
        assertThat(existingReservation.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // When - 1/24~27 연박 예약 시도 (중간에 기존 예약 포함)
        LocalDate newStart = BASE_DATE;                   // 24일
        LocalDate newEnd = BASE_DATE.plusDays(3);         // 27일

        ExtractableResponse<Response> response = 예약_요청(
                siteNumber, "박민수", "010-9999-8888", newStart, newEnd);

        // Then - 예약 실패해야 함
        assertThat(response.statusCode())
                .as("중간 날짜에 기존 예약이 있으면 연박 예약이 실패해야 한다")
                .isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("기존 예약 종료일 다음날부터 예약하면 성공한다")
    void 기존_예약_종료일_다음날부터_예약하면_성공한다() {
        // Given - B-5에 예약 존재
        LocalDate existingStart = BASE_DATE.plusDays(10);
        LocalDate existingEnd = BASE_DATE.plusDays(11);
        String siteNumber = "B-5";

        ExtractableResponse<Response> existingReservation = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", existingStart, existingEnd);
        assertThat(existingReservation.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // When - 기존 예약 종료일 다음날부터 예약
        LocalDate newStart = existingEnd;                 // 종료일 다음날
        LocalDate newEnd = existingEnd.plusDays(1);

        ExtractableResponse<Response> response = 예약_요청(
                siteNumber, "박민수", "010-9999-8888", newStart, newEnd);

        // Then - 예약 성공
        assertThat(response.statusCode())
                .as("기존 예약 종료일 다음날부터는 예약이 가능해야 한다")
                .isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("기존 예약 시작일 전날까지 예약하면 성공한다")
    void 기존_예약_시작일_전날까지_예약하면_성공한다() {
        // Given - B-5에 예약 존재
        LocalDate existingStart = BASE_DATE.plusDays(20);
        LocalDate existingEnd = BASE_DATE.plusDays(21);
        String siteNumber = "B-5";

        ExtractableResponse<Response> existingReservation = 예약_요청(
                siteNumber, "홍길동", "010-1234-5678", existingStart, existingEnd);
        assertThat(existingReservation.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // When - 기존 예약 시작일 전날까지 예약
        LocalDate newStart = existingStart.minusDays(2);
        LocalDate newEnd = existingStart;                 // 시작일 전날까지

        ExtractableResponse<Response> response = 예약_요청(
                siteNumber, "박민수", "010-9999-8888", newStart, newEnd);

        // Then - 예약 성공
        assertThat(response.statusCode())
                .as("기존 예약 시작일 전날까지는 예약이 가능해야 한다")
                .isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("중간 날짜에 예약이 있는 사이트는 검색 결과에서 제외된다")
    void 중간_날짜에_예약이_있는_사이트는_검색_결과에서_제외된다() {
        // Given - B-6에 예약 존재
        LocalDate existingStart = BASE_DATE.plusDays(31);
        LocalDate existingEnd = BASE_DATE.plusDays(32);

        ExtractableResponse<Response> existingReservation = 예약_요청(
                "B-6", "홍길동", "010-1234-5678", existingStart, existingEnd);
        assertThat(existingReservation.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // When - 기존 예약을 포함하는 기간으로 검색
        LocalDate searchStart = BASE_DATE.plusDays(30);
        LocalDate searchEnd = BASE_DATE.plusDays(33);

        List<Map<String, Object>> searchResults = 가용_사이트_검색(searchStart, searchEnd);

        // Then - B-6은 검색 결과에서 제외되어야 함
        boolean containsB6 = searchResults.stream()
                .anyMatch(site -> "B-6".equals(site.get("siteNumber")));

        assertThat(containsB6)
                .as("중간 날짜에 예약이 있는 B-6 사이트는 검색 결과에서 제외되어야 한다")
                .isFalse();
    }
}
