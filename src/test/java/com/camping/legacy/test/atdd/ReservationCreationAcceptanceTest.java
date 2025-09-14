package com.camping.legacy.test.atdd;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;

@DisplayName("예약 생성 테스트")
public class ReservationCreationAcceptanceTest extends AcceptanceTestBase {

    /**
     * Scenario: 정상적인 예약 생성
     *
     * Given 예약시작일은 오늘 날짜 + 4일, 예약종료일은 오늘 날짜 + 6일이다
     * and A-1 사이트는 공실이다.
     *
     * When 고객이 해당 예약 기간에 A-1 사이트를 예약한다
     *
     * Then 예약이 성공적으로 생성된다
     * and HTTP 상태 코드는 201이다
     * and 예약 상태는 "CONFIRMED"이다
     * and 예약 완료 시 6자리 영숫자 확인 코드 자동 생성된다.
     */
    @Test
    @DisplayName("유효한 날짜로 예약 신청시, 예약이 성공한다.")
    void 정상적인_예약_생성_테스트() {
        // Given: 오늘 날짜는 2024-12-01이

        // When: 고객이 2024-12-05부터 2024-12-07까지 A-1 사이트를 예약한다
        Map<String, Object> request = new HashMap<>();
        request.put("customerName", "홍길동");
        request.put("startDate", "2024-12-05");
        request.put("endDate", "2024-12-07");
        request.put("siteNumber", "A-1");
        request.put("phoneNumber", "010-1234-5678");
        request.put("numberOfPeople", 4);
        request.put("carNumber", "12가3456");
        request.put("requests", "조용한 구역 부탁드립니다");

        ExtractableResponse<Response> response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(CREATED.value())
                .extract();

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            softly.assertThat(response.jsonPath().getString("confirmationCode")).matches("^[A-Za-z0-9]{6}$");
        });
    }

}
