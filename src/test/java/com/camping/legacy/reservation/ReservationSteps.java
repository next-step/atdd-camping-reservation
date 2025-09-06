package com.camping.legacy.reservation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationSteps {

    public static void 예약_가능한_캠핑_사이트_A001이_존재한다() {
        // db 초기화
        // 혹은 api 호출로 캠핑 사이트 생성
    }

    public static void 오늘_날짜가_설정된다(String today) {
        // 테스트 환경에서는 현재 날짜를 고정하여 사용
    }

    public static ExtractableResponse<Response> 고객이_예약을_요청한다(
            String customerName, String phoneNumber, String startDate,
            String endDate, String siteNumber) {

        var reservationRequest = Map.of(
                "customerName", customerName,
                "phoneNumber", phoneNumber,
                "startDate", startDate,
                "endDate", endDate,
                "siteNumber", siteNumber
        );

        return given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all().extract();
    }

    public static void 예약이_성공적으로_생성된다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
    }

    public static void 확인코드_6자리가_생성된다(ExtractableResponse<Response> response) {
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotNull();
        assertThat(confirmationCode).hasSize(6);
        assertThat(confirmationCode).matches("[A-Z0-9]{6}");
    }

    public static void 예약_상태가_CONFIRMED로_설정된다(ExtractableResponse<Response> response) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo("CONFIRMED");
    }

    public static void 예약이_실패한다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    public static void 오류_메시지가_반환된다(ExtractableResponse<Response> response, String expectedMessage) {
        String actualMessage = response.jsonPath().getString("message");
        assertThat(actualMessage).isEqualTo(expectedMessage);
    }
}
