package com.camping.legacy.test.atdd;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.CREATED;

@DisplayName("예약 생성 테스트")
public class ReservationCreationAcceptanceTest extends AcceptanceTestBase {

    /**
     * Scenario: 정상적인 예약 생성
     * <p>
     * Given 예약기간은 30일 이내 (예약시작일 = 오늘 날짜 + 4일, 예약종료일 = 오늘 날짜 + 6일)
     * and A-1 사이트는 공실이다.
     * <p>
     * When 고객이 해당 예약 기간에 A-1 사이트를 예약한다
     * <p>
     * Then 예약이 성공적으로 생성된다
     * and HTTP 상태 코드는 201이다
     * and 예약 상태는 "CONFIRMED"이다
     * and 예약 완료 시 6자리 영숫자 확인 코드 자동 생성된다.
     */
    @Test
    @DisplayName("유효한 날짜로 예약 신청시, 예약이 생성된다.")
    void 정상적인_예약_생성_테스트() {
        // Given
        var startDate = now().plusDays(4).toString();
        var endDate = now().plusDays(6).toString();

        // When: 고객이 오늘로부터 30일 이내 A-1 사이트를 예약한다
        var request = defaultRequest(startDate, endDate);
        request.put("startDate", startDate);
        request.put("endDate", endDate);

        var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .extract();

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(CREATED.value());
            softly.assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            softly.assertThat(response.jsonPath().getString("confirmationCode")).matches("^[A-Za-z0-9]{6}$");
        });
    }

    private static HashMap<String, Object> defaultRequest(String startDate, String endDate) {
        var request = new HashMap<String, Object>();
        request.put("customerName", "홍길동");
        request.put("startDate", startDate);
        request.put("endDate", endDate);
        request.put("siteNumber", "A-1");
        request.put("phoneNumber", "010-1234-5678");
        request.put("numberOfPeople", 4);
        request.put("carNumber", "12가3456");
        request.put("requests", "조용한 구역 부탁드립니다");
        return request;
    }

}
