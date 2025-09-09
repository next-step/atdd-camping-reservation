package com.camping.legacy.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
class ReservationStayAcceptanceTest extends BaseAcceptanceTest {

    @Test
    void 중간_날짜에_다른_예약이_있으면_예약_불가() {
        // given - A-5 캠핑 구역이 12월 22일부터 23일까지 예약되어 있을 때
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 22);
        LocalDate endDate = LocalDate.of(currentYear, 12, 23);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new HashMap<>();
        existingReservation.put("siteNumber", "A-5");
        existingReservation.put("startDate", startDate.toString());
        existingReservation.put("endDate", endDate.toString());
        existingReservation.put("customerName", "기존예약고객");
        existingReservation.put("phoneNumber", "010-7777-7777");

        given()
                .contentType("application/json")
                .body(existingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());
        
        // when - 고객이 12월 20일부터 24일까지 예약하려고 하면
        startDate = LocalDate.of(currentYear, 12, 20);
        endDate = LocalDate.of(currentYear, 12, 24);
        
        Map<String, Object> newReservation = new HashMap<>();
        newReservation.put("siteNumber", "A-5");
        newReservation.put("startDate", startDate.toString());
        newReservation.put("endDate", endDate.toString());
        newReservation.put("customerName", "연박예약고객");
        newReservation.put("phoneNumber", "010-6666-6666");
        
        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(newReservation)
        .when()
                .post("/api/reservations")
        .then()
                .extract();
        
        // then - 예약할 수 없다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 날짜_선택_오류_시_예약_불가() {
        // when - 고객이 체크아웃 날짜를 체크인 날짜보다 앞선 날로 선택하면
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 10);
        LocalDate endDate = LocalDate.of(currentYear, 12, 8);

        Map<String, Object> invalidReservation = new HashMap<>();
        invalidReservation.put("siteNumber", "A-7");
        invalidReservation.put("startDate", startDate.toString());
        invalidReservation.put("endDate", endDate.toString());
        invalidReservation.put("customerName", "잘못된날짜고객");
        invalidReservation.put("phoneNumber", "010-5555-5555");
        
        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(invalidReservation)
        .when()
                .post("/api/reservations")
        .then()
                .extract();
        
        // then - 예약할 수 없다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    void 일부_날짜만_가능해도_전체_예약_불가() {
        // given - A-2 캠핑 구역이 12월 20일, 21일은 비어있지만 22일은 예약되어 있을 때
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 12, 22);
        LocalDate endDate = LocalDate.of(currentYear, 12, 23);

        // 기존 예약 생성
        Map<String, Object> existingReservation = new HashMap<>();
        existingReservation.put("siteNumber", "A-2");
        existingReservation.put("startDate", startDate.toString());
        existingReservation.put("endDate", endDate.toString());
        existingReservation.put("customerName", "부분충돌고객");
        existingReservation.put("phoneNumber", "010-3333-3333");

        given()
                .contentType("application/json")
                .body(existingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .statusCode(CREATED.value());

        // when - 고객이 12월 20일부터 22일까지 예약하려고 하면
        startDate = LocalDate.of(currentYear, 12, 20);
        endDate = LocalDate.of(currentYear, 12, 22);

        Map<String, Object> overlappingReservation = new HashMap<>();
        overlappingReservation.put("siteNumber", "A-2");
        overlappingReservation.put("startDate", startDate.toString());
        overlappingReservation.put("endDate", endDate.toString());
        overlappingReservation.put("customerName", "부분예약고객");
        overlappingReservation.put("phoneNumber", "010-2222-2222");

        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(overlappingReservation)
        .when()
                .post("/api/reservations")
        .then()
                .extract();

        // then - 예약할 수 없다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void _30일_이내만_예약_가능() {
        // when - 고객이 오늘로부터 31일 이후의 날짜로 예약하려고 하면
        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = LocalDate.now().plusDays(33);

        Map<String, Object> futureReservation = new HashMap<>();
        futureReservation.put("siteNumber", "A-8");
        futureReservation.put("startDate", startDate.toString());
        futureReservation.put("endDate", endDate.toString());
        futureReservation.put("customerName", "미래예약고객");
        futureReservation.put("phoneNumber", "010-4444-4444");

        ExtractableResponse<Response> response = given()
                .contentType("application/json")
                .body(futureReservation)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        // then - "예약은 30일 이내에만 가능합니다"라는 안내 메시지가 나타난다
        assertThat(response.statusCode()).isEqualTo(CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약은 30일 이내에만 가능합니다");
    }
}
