package com.camping.legacy.acceptance.reservation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.List;

import static com.camping.legacy.acceptance.reservation.ReservationTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationTestHelpers {

    // =====================================================
    // 응답 데이터 추출
    // =====================================================

    public static String 예약정보에서_확인코드_조회(ExtractableResponse<Response> response) {
        return response.jsonPath().getString("confirmationCode");
    }

    public static Long 예약정보에서_예약ID_조회(ExtractableResponse<Response> response) {
        return response.jsonPath().getLong("id");
    }

    // =====================================================
    // 예약 상태 검증
    // =====================================================

    public static void 예약이_되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(201);
    }

    public static void 예약이_되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(409);
    }

    public static void 예약이_수정되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(200);
    }

    public static void 예약이_수정되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(400);
    }

    public static void 예약이_취소되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(200);
    }

    public static void 예약이_취소되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(400);
    }

    // =====================================================
    // 예약 상태 확인
    // =====================================================

    public static void 예약상태가_확정이다(ExtractableResponse<Response> response, String expectedStatus) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo(expectedStatus);
    }

    public static void 사전예약_취소_상태이다(ExtractableResponse<Response> response) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo(예약상태_사전취소);
    }

    public static void 당일예약_취소_상태이다(ExtractableResponse<Response> response) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo(예약상태_당일취소);
    }

    // =====================================================
    // 예약 정보 검증
    // =====================================================

    public static void 확인코드가_발급되었다(ExtractableResponse<Response> response, int expectedLength) {
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).hasSize(expectedLength);
    }

    public static void 확인코드가_유지되었다(ExtractableResponse<Response> response, String expectedConfirmationCode) {
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isEqualTo(expectedConfirmationCode);
    }

    public static void 예약날짜가_변경되었다(ExtractableResponse<Response> response, String expectedStartDate, String expectedEndDate) {
        String startDate = response.jsonPath().getString("startDate");
        String endDate = response.jsonPath().getString("endDate");
        assertThat(startDate).isEqualTo(expectedStartDate);
        assertThat(endDate).isEqualTo(expectedEndDate);
    }

    // =====================================================
    // 요금 및 포인트 검증
    // =====================================================

    public static void 예약_금액이_일치한다(ExtractableResponse<Response> response, int expectedPrice) {
        Integer totalPrice = response.jsonPath().getInt("totalPrice");
        assertThat(totalPrice).isEqualTo(expectedPrice);
    }

    public static void 포인트가_일치한다(ExtractableResponse<Response> response, int expectedPoints) {
        Integer earnedPoints = response.jsonPath().getInt("earnedPoints");
        assertThat(earnedPoints).isEqualTo(expectedPoints);
    }

    // =====================================================
    // 사이트 검증
    // =====================================================

    public static void 사이트가_존재한다(ExtractableResponse<Response> response, String expectedSiteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber", String.class);
        assertThat(siteNumbers).contains(expectedSiteNumber);
    }
}