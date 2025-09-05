package com.camping.legacy.acceptance.reservation;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.response.Response;
import org.assertj.core.api.AbstractIntegerAssert;
import org.springframework.http.HttpStatus;

public class ReservationAcceptanceTestSteps {

    // 예약 생성
    public static Response 예약_생성을_요청한다(ReservationRequest request) {
        return given().body(request)
            .when().post("/api/reservations")
            .thenReturn();
    }

    public static AbstractIntegerAssert<?> 예약_생성이_성공한다(Response 예약_생성_응답) {
        return assertThat(예약_생성_응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 예약_생성이_실패한다(Response 예약_생성_응답) {
        assertThat(예약_생성_응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
