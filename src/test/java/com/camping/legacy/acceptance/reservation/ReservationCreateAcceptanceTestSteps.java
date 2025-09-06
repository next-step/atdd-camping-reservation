package com.camping.legacy.acceptance.reservation;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import io.restassured.response.Response;
import org.assertj.core.api.AbstractIntegerAssert;
import org.springframework.http.HttpStatus;

public class ReservationCreateAcceptanceTestSteps {

    public static ReservationResponse 예약이_생성되어있다(ReservationRequest request) {
        Response 예약_생성_응답 = 예약_생성을_요청한다(request);
        예약_생성이_성공한다(예약_생성_응답);
        return 예약_생성_응답을_가져온다(예약_생성_응답);
    }

    public static Response 예약_생성을_요청한다(ReservationRequest request) {
        return given().body(request)
            .when().post("/api/reservations")
            .thenReturn();
    }

    public static AbstractIntegerAssert<?> 예약_생성이_성공한다(Response 예약_생성_응답) {
        return assertThat(예약_생성_응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 확인_코드가_6자리_영숫자로_생성된다(Response 예약_생성_응답) {
        String confirmationCode = 예약_생성_응답.jsonPath().getString("confirmationCode");

        assertThat(confirmationCode)
            .hasSize(6)
            .matches("^[A-Z0-9]{6}$");
    }

    public static void 예약_생성이_실패한다(Response 예약_생성_응답, String expectedErrorMessage) {
        assertThat(예약_생성_응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        String actualErrorMessage = 예약_생성_응답.jsonPath().getString("message");
        assertThat(actualErrorMessage).isEqualTo(expectedErrorMessage);
    }

    public static ReservationResponse 예약_생성_응답을_가져온다(Response 예약_생성_응답) {
        return 예약_생성_응답.as(ReservationResponse.class);
    }
}
