package com.camping.legacy.assertion;

import static com.camping.legacy.fixture.ReservationFixture.CONFIRMED_STATUS;
import static org.assertj.core.api.Assertions.*;

import com.camping.legacy.client.ReservationClient;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

public class ReservationAssertion {
    public static void 예약_상태가_변경됨을_확인한다(Long id, String expectedStatus) {
        var response = ReservationClient.예약을_조회한다(id);

        String actualStatus = response.jsonPath().getString("[0].status");
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    public static void 예약_취소가_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    public static void 메시지가_확인된다(ExtractableResponse<Response> response, String expected) {
        String message = response.jsonPath().getString("message");
        assertThat(message).isNotNull();
        assertThat(message).isEqualTo(expected);
    }

    public static void 예약_확인코드가_발급되었다(ExtractableResponse<Response> response) {
        String code = response.jsonPath().getString("confirmationCode");
        assertThat(code).isNotNull();
        assertThat(code).hasSize(6);
        assertThat(code).matches("^[A-Z0-9]*$");
    }

    public static void 예약이_성공적으로_생성되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 예약이_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    public static void 예약_결과를_확인한다(ExtractableResponse<Response> response, HttpStatus expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
    }

    public static void 예약이_확정된_상태이다(ExtractableResponse<Response> response) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(CONFIRMED_STATUS);
    }

    public static void 예약_비용을_확인한다(ExtractableResponse<Response> response, int expected) {
        assertThat(response.jsonPath().getInt("totalPrice")).isEqualTo(expected);
    }
}
