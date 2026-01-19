package com.camping.legacy.common;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 관련 Custom Assertions
 */
public class ReservationAssertions {

    private static final int CONFIRMATION_CODE_LENGTH = 6;

    /**
     * 예약 생성 성공 검증
     */
    public static void 예약_생성_성공_검증(ExtractableResponse<Response> response, String expectedCustomerName) {
        assertThat(response.statusCode())
                .as("예약 생성이 성공해야 합니다")
                .isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode"))
                .as("확인 코드가 생성되어야 합니다")
                .hasSize(CONFIRMATION_CODE_LENGTH);
        assertThat(response.jsonPath().getString("customerName"))
                .as("고객명이 일치해야 합니다")
                .isEqualTo(expectedCustomerName);
    }

    /**
     * 예약 생성 성공 검증 (고객명 검증 없이)
     */
    public static void 예약_생성_성공_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("예약 생성이 성공해야 합니다")
                .isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode"))
                .as("확인 코드가 생성되어야 합니다")
                .hasSize(CONFIRMATION_CODE_LENGTH);
    }

    /**
     * 예약 수정 성공 검증
     */
    public static void 예약_수정_성공_검증(ExtractableResponse<Response> response,
                                     String expectedStartDate, String expectedEndDate) {
        assertThat(response.statusCode())
                .as("예약 수정이 성공해야 합니다")
                .isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("startDate"))
                .as("시작일이 변경되어야 합니다")
                .isEqualTo(expectedStartDate);
        assertThat(response.jsonPath().getString("endDate"))
                .as("종료일이 변경되어야 합니다")
                .isEqualTo(expectedEndDate);
    }

    /**
     * 예약 충돌 오류 검증
     */
    public static void 예약_충돌_오류_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("예약 충돌로 거부되어야 합니다")
                .isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message"))
                .as("충돌 오류 메시지가 반환되어야 합니다")
                .isEqualTo("해당 기간에 이미 예약이 존재합니다");
    }

    /**
     * 예약 수정 거부 검증
     */
    public static void 예약_수정_거부_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("예약 수정이 거부되어야 합니다")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 취소된 예약 수정 시도 거부 검증
     */
    public static void 취소된_예약_수정_거부_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode())
                .as("취소된 예약 수정이 거부되어야 합니다")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message"))
                .as("취소된 예약 수정 오류 메시지가 반환되어야 합니다")
                .isEqualTo("취소된 예약은 수정할 수 없습니다");
    }
}