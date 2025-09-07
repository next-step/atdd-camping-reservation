package com.camping.legacy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
class CampingReservationAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 예약_생성_성공_시_예약_번호_받기() {
        // given - A-2 캠핑 구역이 12월 25일부터 27일까지 비어있을 때
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = LocalDate.now().plusDays(9);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("siteNumber", "A-2");
        reservationRequest.put("startDate", startDate.toString());
        reservationRequest.put("endDate", endDate.toString());
        reservationRequest.put("customerName", "김테스트");
        reservationRequest.put("phoneNumber", "010-1234-5678");
        
        // when - 고객이 A-2 캠핑 구역을 예약하면
        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(reservationRequest)
        .when()
                .post("/api/reservations")
        .then()
                .extract();
                
        // then - 예약이 완료되고 6자리 예약 확인 번호를 받는다
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("김테스트");
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-2");
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(startDate.toString());
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(endDate.toString());
    }
}
