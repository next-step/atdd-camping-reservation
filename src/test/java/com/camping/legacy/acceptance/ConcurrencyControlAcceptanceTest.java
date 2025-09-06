package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static com.camping.legacy.acceptance.AcceptanceTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("동시성 제어")
class ConcurrencyControlAcceptanceTest extends AcceptanceCommon {

    @Test
    @DisplayName("여러 회원이 동일 사이트를 동일 날짜에 동시에 예약을 할 시 1명만 성공한다")
    void 동시_예약_1명만_성공() {
        // Given 특정 날짜에 예약되지 않은 사이트가 존재한다.
        ReservationRequest request = createReservationRequest();
        ReservationRequest sameRequest = createSameReservationRequest();

        // When 동시에 동일 사이트 동일 날짜 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
        ExtractableResponse<Response> sameResponse = getCreateReservationApiResponse(sameRequest);

        // Then 1명은 성공하고, 나머지는 "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(201);
        JsonPath jsonPath = response.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);
        assertThat(jsonPath.getString("confirmationCode")).matches("[A-Z0-9]+");

        assertThat(sameResponse.statusCode()).isEqualTo(409);
        assertThat(sameResponse.body().asString()).contains("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("여러 회원이 동일 사이트를 취소된 예약이 존재하는 동일 날짜에 동시에 예약을 할 시 1명만 성공한다")
    void 동시_예약_취소된_예약_존재_시_1명만_성공() {
        // Given 특정 날짜의 취소된 예약이 존재하는 사이트가 존재한다.
        ReservationRequest request = createCancelledReservationRequest();
        ReservationRequest sameRequest = createCancelledSameReservationRequest();

        // When 동시에 동일 사이트 동일 날짜 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
        ExtractableResponse<Response> sameResponse = getCreateReservationApiResponse(sameRequest);

        // Then 1명은 성공하고, 나머지는 "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(201);
        JsonPath jsonPath = response.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);
        assertThat(jsonPath.getString("confirmationCode")).matches("[A-Z0-9]+");

        assertThat(sameResponse.statusCode()).isEqualTo(409);
        assertThat(sameResponse.body().asString()).contains("해당 기간에 이미 예약이 존재합니다.");
    }
}