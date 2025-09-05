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
import org.springframework.transaction.annotation.Transactional;

import static com.camping.legacy.acceptance.AcceptanceTestFixTure.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("예약 생성")
class ReservationCreationAcceptanceTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("예약을 한 날짜가 예약한 날짜와 30일 이내 차이가 나면 예약이 성공한다")
    void 예약_성공_30일_이내() {
        // Given 특정 날짜와 오늘이 30일 이내 차이 나는 사이트가 존재한다 (+ 모든 값이 채워진).
        ReservationRequest request = createReservationRequest();

        // When 회원이 특정 날짜의 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);

        // Then 6자리 영숫자 확인 코드가 생성된다.
        // And 예약이 생성된다(예약 상태가 "CONFIRMED").
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
    @DisplayName("예약을 한 날짜가 예약한 날짜와 30일 초과 차이가 나면 예외가 발생한다")
    void 예약_실패_30일_초과() {
        // Given 특정 날짜와 오늘이 30일 초과 차이 나는 사이트가 존재한다.
        ReservationRequest request = createWrongReservationRequest();

        // When 회원이 특정 날짜의 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);

        // Then "예약은 최대 30일 전까지만 가능합니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body().asString()).contains("예약은 최대 30일 전까지만 가능합니다");
    }

    @Test
    @DisplayName("이미 예약된 사이트 예약을 한 경우 예외가 발생한다")
    void 예약_실패_이미_예약된_사이트() {
        // Given 특정 날짜가 예약된 사이트가 존재한다.
        예약_생성_성공();
        ReservationRequest request = createBookedReservationRequest();

        // When 회원이 특정 날짜의 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);

        // Then "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body().asString()).contains("해당 기간에 이미 예약이 존재합니다.");
    }
}