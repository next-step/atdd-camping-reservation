package com.camping.legacy.acceptance.reservation;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.response.Response;
import org.assertj.core.api.AbstractIntegerAssert;
import org.springframework.http.HttpStatus;

public class ReservationUpdateAcceptanceTestSteps {

    public static Response 예약_수정을_요청한다(
        Long 예약_ID, String 확인_코드, ReservationRequest request
    ) {
        return given().body(request)
            .when().put("/api/reservations/%d?confirmationCode=%s".formatted(예약_ID, 확인_코드))
            .thenReturn();
    }

    public static AbstractIntegerAssert<?> 예약_수정이_성공한다(Response 예약_수정_응답) {
        return assertThat(예약_수정_응답.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 예약_수정이_실패한다(Response 예약_수정_응답, String expectedErrorMessage) {
        assertThat(예약_수정_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        String actualErrorMessage = 예약_수정_응답.jsonPath().getString("message");
        assertThat(actualErrorMessage).isEqualTo(expectedErrorMessage);
    }
}
