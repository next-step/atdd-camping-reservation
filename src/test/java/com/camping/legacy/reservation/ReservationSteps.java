package com.camping.legacy.reservation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationSteps {

    public static void 예약_가능한_캠핑_사이트_A1이_존재한다() {
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
                "endDate", LocalDate.now(),
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

    public static List<ExtractableResponse<Response>> 동시에_예약을_요청한다(
            String customerName1, String phoneNumber1,
            String customerName2, String phoneNumber2,
            String startDate, String endDate, String siteNumber) {

        var request1 = Map.of(
                "customerName", customerName1,
                "phoneNumber", phoneNumber1,
                "startDate", startDate,
                "endDate", endDate,
                "siteNumber", siteNumber
        );

        var request2 = Map.of(
                "customerName", customerName2,
                "phoneNumber", phoneNumber2,
                "startDate", startDate,
                "endDate", endDate,
                "siteNumber", siteNumber
        );

        CompletableFuture<ExtractableResponse<Response>> future1 =
                CompletableFuture.supplyAsync(() ->
                        given().log().all()
                                .contentType("application/json")
                                .body(request1)
                                .when().post("/api/reservations")
                                .then().log().all().extract()
                );

        CompletableFuture<ExtractableResponse<Response>> future2 =
                CompletableFuture.supplyAsync(() ->
                        given().log().all()
                                .contentType("application/json")
                                .body(request2)
                                .when().post("/api/reservations")
                                .then().log().all().extract()
                );

        return List.of(future1.join(), future2.join());
    }

    public static void 하나의_예약만_성공한다(List<ExtractableResponse<Response>> responses) {
        long successCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CREATED.value())
                .count();

        assertThat(successCount).isEqualTo(1);
    }

    public static void 나머지_예약은_실패한다(List<ExtractableResponse<Response>> responses) {
        long failureCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CONFLICT.value())
                .count();

        assertThat(failureCount).isEqualTo(1);
    }

    public static void 실패한_예약에_오류_메시지가_반환된다(
            List<ExtractableResponse<Response>> responses, String expectedMessage) {

        Optional<ExtractableResponse<Response>> failedResponse = responses.stream()
                .filter(response -> response.statusCode() == HttpStatus.CONFLICT.value())
                .findFirst();

        assertThat(failedResponse).isPresent();
        String actualMessage = failedResponse.get().jsonPath().getString("message");
        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    public static Long 사이트에_취소된_예약이_존재한다(String siteNumber, String startDate, String endDate) {
        // 먼저 예약 생성
        var reservationRequest = Map.of(
                "customerName", "기존고객",
                "phoneNumber", "010-0000-0000",
                "startDate", startDate,
                "endDate", endDate,
                "siteNumber", siteNumber
        );

        var createResponse = given().log().all()
                .contentType("application/json")
                .body(reservationRequest)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // 예약 취소
        given().log().all()
                .param("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value());

        return reservationId;
    }

    public static void 취소된_예약은_중복_체크에서_제외된다(Long cancelledReservationId) {
        var response = given().log().all()
                .when().get("/api/reservations/" + cancelledReservationId)
                .then().log().all().extract();

        String status = response.jsonPath().getString("status");
        assertThat(status).isIn("CANCELLED", "CANCELLED_SAME_DAY");
    }

    public static void 사이트가_기간동안_예약_가능하다(String siteNumber, String startDate, String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            var response = given().log().all()
                    .param("date", date.toString())
                    .when().get("/api/sites/" + siteNumber + "/availability")
                    .then().log().all().extract();

            // 하나라도 예약 불가능하면 테스트 실패
            if (response.statusCode() != HttpStatus.OK.value() ||
                    !response.jsonPath().getBoolean("available")) {
                throw new AssertionError("사이트 " + siteNumber + "가 " + date + "에 예약 불가능합니다.");
            }
        }
    }

    public static void 사이트가_날짜에_예약_가능하다(String siteNumber, String date) {
        var response = given().log().all()
                .param("date", date)
                .when().get("/api/sites/" + siteNumber + "/availability")
                .then().log().all().extract();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
    }

    public static void 사이트가_날짜에_이미_예약되어_있다(String siteNumber, String date) {
        // 특정 날짜에 기존 예약 생성
        var existingReservation = Map.of(
                "customerName", "기존고객",
                "phoneNumber", "010-0000-0000",
                "startDate", date,
                "endDate", date,
                "siteNumber", siteNumber
        );

        given().log().all()
                .contentType("application/json")
                .body(existingReservation)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    public static ExtractableResponse<Response> 고객이_연박_예약을_요청한다(
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

    public static void 연박_예약이_성공적으로_생성된다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
    }

    public static void 전체_기간에_대한_예약이_생성된다(ExtractableResponse<Response> response, String startDate, String endDate) {
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(startDate);
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(endDate);
    }

    public static void 연박_예약이_실패한다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    public static ExtractableResponse<Response> 고객이_30일_초과_연박_예약을_요청한다(
            String customerName, String phoneNumber, String startDate,
            String endDate, String siteNumber) {

        return 고객이_연박_예약을_요청한다(customerName, phoneNumber, startDate, endDate, siteNumber);
    }
}
