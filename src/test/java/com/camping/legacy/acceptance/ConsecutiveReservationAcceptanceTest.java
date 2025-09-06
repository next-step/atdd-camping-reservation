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

@DisplayName("연박 예약")
class ConsecutiveReservationAcceptanceTest extends AcceptanceCommon {

    @Test
    @DisplayName("시작일과 종료일을 지정한 특정 기간에 연박 예약을 할 수 있다")
    void 연박_예약_성공() {
        // Given 연박 기간동안 예약되지 않은 사이트가 존재한다.
        ReservationRequest request = createConsecutiveReservationRequest();

        // When 회원이 연박 기간동안 예약을 한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);

        // Then 예약이 생성된다.
        assertThat(response.statusCode()).isEqualTo(201);
        JsonPath jsonPath = response.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("customerName")).isEqualTo(request.getCustomerName());
        assertThat(jsonPath.getString("startDate")).isEqualTo(request.getStartDate().toString());
        assertThat(jsonPath.getString("endDate")).isEqualTo(request.getEndDate().toString());
        assertThat(jsonPath.getString("siteNumber")).isEqualTo(request.getSiteNumber());
        assertThat(jsonPath.getString("phoneNumber")).isEqualTo(request.getPhoneNumber());
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);
        assertThat(jsonPath.getString("confirmationCode")).matches("[A-Z0-9]+");
        assertThat(jsonPath.getString("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("특정 기간에 연박 예약 시 취소된 예약과 예약이 없는 경우에도 연박 예약을 할 수 있다")
    void 연박_예약_취소된_예약_포함_성공() {
        // Given 연박 기간동안 취소된 예약과 예약되지 않은 사이트가 존재한다.
        ReservationRequest request = createConsecutiveWithCancelledReservationRequest();

        // When 회원이 연박 기간동안 예약을 한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);

        // Then 예약이 생성된다.
        assertThat(response.statusCode()).isEqualTo(201);
        JsonPath jsonPath = response.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("customerName")).isEqualTo(request.getCustomerName());
        assertThat(jsonPath.getString("startDate")).isEqualTo(request.getStartDate().toString());
        assertThat(jsonPath.getString("endDate")).isEqualTo(request.getEndDate().toString());
        assertThat(jsonPath.getString("siteNumber")).isEqualTo(request.getSiteNumber());
        assertThat(jsonPath.getString("phoneNumber")).isEqualTo(request.getPhoneNumber());
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);
        assertThat(jsonPath.getString("confirmationCode")).matches("[A-Z0-9]+");
        assertThat(jsonPath.getString("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("연박 예약 중 연박 기간에 취소되지 않은 예약이 존재하면 예외가 발생한다")
    void 연박_예약_실패_기존_예약_존재() {
        // Given 연박 기간동안 일부 or 전체 예약된 사이트가 존재한다.
        ReservationRequest existingRequest = createExistingReservationInConsecutivePeriod();
        ExtractableResponse<Response> existingResponse = getCreateReservationApiResponse(existingRequest);
        assertThat(existingResponse.statusCode()).isEqualTo(201);

        ReservationRequest request = createBlockedConsecutiveReservationRequest();

        // When 회원이 연박 기간동안 예약을 한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);

        // Then "선택한 기간에 이미 예약된 날짜가 있습니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body().asString()).contains("해당 기간에 이미 예약이 존재합니다.");
    }
}