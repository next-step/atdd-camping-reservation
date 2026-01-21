package com.camping.legacy.common;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 관련 Custom Assertions
 */
public class ReservationAssertions {

    /**
     * 예약 생성 성공 검증
     */
    public static void 예약_생성_성공_검증(ExtractableResponse<Response> response, String expectedCustomerName) {
        assertThat(response.statusCode())
                .as("예약 생성 상태 코드")
                .isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode"))
                .as("확인 코드는 6자리")
                .hasSize(6);
        assertThat(response.jsonPath().getString("customerName"))
                .as("고객명")
                .isEqualTo(expectedCustomerName);
    }

    /**
     * 예약 생성 성공 검증 (고객명 검증 제외)
     */
    public static void 예약_생성_성공_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("예약 생성 상태 코드")
                .isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode"))
                .as("확인 코드는 6자리")
                .hasSize(6);
    }

    /**
     * 예약 수정 성공 검증
     */
    public static void 예약_수정_성공_검증(ExtractableResponse<Response> response,
                                        String expectedStartDate, String expectedEndDate) {
        assertThat(response.statusCode())
                .as("예약 수정 상태 코드")
                .isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("startDate"))
                .as("시작일")
                .isEqualTo(expectedStartDate);
        assertThat(response.jsonPath().getString("endDate"))
                .as("종료일")
                .isEqualTo(expectedEndDate);
    }

    /**
     * 예약 취소 성공 검증
     */
    public static void 예약_취소_성공_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("예약 취소 상태 코드")
                .isIn(HttpStatus.OK.value(), HttpStatus.NO_CONTENT.value());
    }

    /**
     * 요청 거부 검증 (4xx 응답)
     */
    public static void 요청_거부_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("요청 거부 상태 코드")
                .isBetween(400, 499);
    }

    /**
     * 충돌 검증 (409 Conflict)
     */
    public static void 충돌_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("충돌 상태 코드")
                .isEqualTo(HttpStatus.CONFLICT.value());
    }
}
