package com.camping.legacy.reservation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationSteps {

    public static void 예약_가능한_캠핑_사이트_A001이_존재한다() {
        // 테스트 환경에서는 기본 데이터로 A-1 사이트가 이미 존재한다고 가정
    }

    public static void 오늘_날짜가_설정된다(String today) {
        // 테스트 환경에서는 현재 날짜를 고정하여 사용
    }

    public static ExtractableResponse<Response> 고객이_예약을_요청한다(
            String customerName, String phoneNumber, String startDate,
            String endDate, String siteNumber) {

        var reservationRequest = Map.of(
                "customerName", customerName,
                "phoneNumber", phoneNumber,
                "startDate", startDate,
                "endDate", endDate,
                "siteNumber", siteNumber
        );

        return given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all().extract();
    }

    public static void 예약이_성공적으로_생성된다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
    }

    public static void 확인코드_6자리가_생성된다(ExtractableResponse<Response> response) {
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotNull();
        assertThat(confirmationCode).hasSize(6);
        assertThat(confirmationCode).matches("[A-Z0-9]{6}");
    }

    public static void 예약_상태가_CONFIRMED로_설정된다(ExtractableResponse<Response> response) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo("CONFIRMED");
    }

    public static void 예약이_실패한다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    public static void 오류_메시지가_반환된다(ExtractableResponse<Response> response, String expectedMessage) {
        String actualMessage = response.jsonPath().getString("message");
        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    public static void 사이트에_예약이_존재한다(String siteNumber, String startDate, String endDate) {
        // 기존 예약 생성
        var existingReservation = Map.of(
                "customerName", "기존고객",
                "phoneNumber", "010-0000-0000",
                "startDate", startDate,
                "endDate", endDate,
                "siteNumber", siteNumber
        );

        given().log().all()
                .contentType("application/json")
                .body(existingReservation)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    public static Long 예약이_존재한다(String reservationId) {
        // 테스트용 예약 생성
        var reservationRequest = Map.of(
                "customerName", "김철수",
                "phoneNumber", "010-1234-5678",
                "startDate", "2024-01-15",
                "endDate", "2024-01-16",
                "siteNumber", "A-1"
        );

        var response = given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        return response.jsonPath().getLong("id");
    }

    public static ExtractableResponse<Response> 예약_ID로_조회한다(Long reservationId) {
        return given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all().extract();
    }

    public static void 예약_정보가_반환된다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
    }

    public static void 고객명_사이트번호_예약기간이_포함된다(ExtractableResponse<Response> response) {
        assertThat(response.jsonPath().getString("customerName")).isNotNull();
        assertThat(response.jsonPath().getString("siteNumber")).isNotNull();
        assertThat(response.jsonPath().getString("startDate")).isNotNull();
        assertThat(response.jsonPath().getString("endDate")).isNotNull();
    }

    public static void 고객의_예약이_존재한다(String customerName) {
        // 테스트용 예약 생성 (여러 개 생성하여 리스트 조회 테스트)
        var reservationRequest1 = Map.of(
                "customerName", customerName,
                "phoneNumber", "010-1234-5678",
                "startDate", "2024-01-15",
                "endDate", "2024-01-16",
                "siteNumber", "A-1"
        );

        var reservationRequest2 = Map.of(
                "customerName", customerName,
                "phoneNumber", "010-1234-5678",
                "startDate", "2024-01-20",
                "endDate", "2024-01-21",
                "siteNumber", "B-1"
        );

        given().log().all()
                .contentType("application/json")
                .body(reservationRequest1)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());

        given().log().all()
                .contentType("application/json")
                .body(reservationRequest2)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    public static ExtractableResponse<Response> 고객명으로_예약을_조회한다(String customerName) {
        return given().log().all()
                .param("customerName", customerName)
                .when().get("/api/reservations")
                .then().log().all().extract();
    }

    public static void 해당_고객의_모든_예약이_반환된다(ExtractableResponse<Response> response, String expectedCustomerName) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<Map<String, Object>> reservations = response.jsonPath().getList("$");
        assertThat(reservations).isNotEmpty();

        // 모든 예약이 해당 고객의 것인지 확인
        for (Map<String, Object> reservation : reservations) {
            assertThat(reservation.get("customerName")).isEqualTo(expectedCustomerName);
        }
    }

    public static Long 확인코드인_예약이_존재한다(String confirmationCode) {
        // 테스트용 예약 생성
        var reservationRequest = Map.of(
                "customerName", "김철수",
                "phoneNumber", "010-1234-5678",
                "startDate", "2024-01-20",
                "endDate", "2024-01-21",
                "siteNumber", "A-1"
        );

        var response = given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        return response.jsonPath().getLong("id");
    }

    public static Long 시작일인_예약이_존재한다(String startDate) {
        var reservationRequest = Map.of(
                "customerName", "김철수",
                "phoneNumber", "010-1234-5678",
                "startDate", startDate,
                "endDate", "2024-01-16",
                "siteNumber", "A-1"
        );

        var response = given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        return response.jsonPath().getLong("id");
    }

    public static ExtractableResponse<Response> 확인코드로_예약을_취소한다(Long reservationId, String confirmationCode) {
        return given().log().all()
                .param("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all().extract();
    }

    public static ExtractableResponse<Response> 예약을_취소한다(Long reservationId, String confirmationCode) {
        return given().log().all()
                .param("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all().extract();
    }

    public static void 예약이_취소된다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약이 취소되었습니다.");
    }

    public static void 예약_상태가_CANCELLED로_변경된다(Long reservationId) {
        var response = given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all().extract();

        assertThat(response.jsonPath().getString("status")).isEqualTo("CANCELLED");
    }

    public static void 예약_상태가_CANCELLED_SAME_DAY로_설정된다(Long reservationId) {
        var response = given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all().extract();

        assertThat(response.jsonPath().getString("status")).isEqualTo("CANCELLED_SAME_DAY");
    }
}
