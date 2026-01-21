package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.fixture.TestFixture.*;
import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_단건_조회;
import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_요청;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 조회 인수 테스트
 *
 * 인수 조건:
 * "예약 ID로 예약 정보를 조회할 수 있어야 한다"
 */
@DisplayName("예약 조회 인수 테스트")
class ReservationQueryAcceptanceTest extends AcceptanceTest {

    private static final LocalDate BASE_DATE = LocalDate.now().plusDays(30);

    @Test
    @DisplayName("예약 단건 조회가 성공한다")
    void 예약_단건_조회_성공() {
        // Given - 예약 생성
        LocalDate startDate = BASE_DATE;
        LocalDate endDate = BASE_DATE.plusDays(1);
        String siteNumber = 사이트_A1;

        // 1. 홍길동이 예약 생성
        ExtractableResponse<Response> createResponse = 예약_요청(
                siteNumber, 홍길동, 홍길동_전화번호, startDate, endDate);
        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = createResponse.jsonPath().getLong("id");

        // When - 단건 조회
        ExtractableResponse<Response> response = 예약_단건_조회(reservationId);

        // Then
        assertThat(response.statusCode())
                .as("예약 단건 조회가 성공해야 한다")
                .isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("customerName"))
                .isEqualTo(홍길동);
        assertThat(response.jsonPath().getString("siteNumber"))
                .isEqualTo(siteNumber);
    }

    @Test
    @DisplayName("존재하지 않는 예약 조회시 실패한다")
    void 존재하지_않는_예약_조회_실패() {
        // Given
        Long nonExistentId = 999999L;

        // When
        ExtractableResponse<Response> response = 예약_단건_조회(nonExistentId);

        // Then
        assertThat(response.statusCode())
                .as("존재하지 않는 예약 조회시 404 응답해야 한다")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}
