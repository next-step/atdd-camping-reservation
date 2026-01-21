package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.fixture.TestFixture.*;
import static com.camping.legacy.acceptance.steps.ReservationSteps.예약_요청;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 입력값 유효성 검증 인수 테스트
 *
 * 인수 조건:
 * "잘못된 입력값으로 예약 시도시 적절한 오류 응답을 반환해야 한다"
 */
@DisplayName("입력값 유효성 검증 인수 테스트")
class ValidationAcceptanceTest extends AcceptanceTest {

    private static final LocalDate BASE_DATE = LocalDate.now().plusDays(30);

    @ParameterizedTest(name = "{2}")
    @DisplayName("예약 기간 경계값 검증")
    @CsvSource({
            "-1, 0, 과거 날짜 예약 실패",
            "1, 35, 30일 초과 기간 예약 실패",
            "5, 3, 종료일이 시작일 이전이면 실패"
    })
    void 예약_기간_경계값(int startOffset, int endOffset, String caseDescription) {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(startOffset);
        LocalDate endDate = LocalDate.now().plusDays(endOffset);

        // When
        ExtractableResponse<Response> response = 예약_요청(
                사이트_A1, 홍길동, 홍길동_전화번호, startDate, endDate);

        // Then
        assertThat(response.statusCode())
                .as(caseDescription)
                .isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("존재하지 않는 사이트로 예약 시도시 실패한다")
    void 존재하지_않는_사이트_예약_실패() {
        // Given
        LocalDate startDate = BASE_DATE;
        LocalDate endDate = BASE_DATE.plusDays(1);

        // When
        ExtractableResponse<Response> response = 예약_요청(
                존재하지_않는_사이트, 홍길동, 홍길동_전화번호, startDate, endDate);

        // Then
        assertThat(response.statusCode())
                .as("존재하지 않는 사이트로 예약 시도시 실패해야 한다")
                .isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.NOT_FOUND.value(), HttpStatus.CONFLICT.value());
    }
}
